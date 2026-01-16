package uk.co.sexeys;

// https://virtualregatta.zendesk.com/hc/en-us/articles/5402546102546--New-Energy-management

public class Stamina {
    final float globalFactor = 0.5f;  // https://forum.virtualregatta.com/topic/21411-gestion-de-l%C3%A9nergie-formule-pour-calculer-la-fatigue/
    public float currentStamina = 1.0f;
    public long penaltyEndTime = 0l;
    final float tackEnergy = 0.1f;
    final float gybeEnergy = 0.1f;
    final float sailEnergy = 0.2f;
    final float weightFactor = 0.92f; // because the boat is heavier on passage than when the gribs were measured

    // for values see https://virtualregatta.zendesk.com/hc/en-us/articles/115001441474-Maneuver-its-boat
    final float speedFactor = 0.5f;
    final long tackPenalty10 = 5*phys.msPerMinute ;
    final long gybePenalty10 = 5*phys.msPerMinute ;
    final long sailPenalty10 = 7*phys.msPerMinute ;
    final long tackPenalty30 = 11*phys.msPerMinute ;
    final long gybePenalty30 = 11*phys.msPerMinute ;
    final long sailPenalty30 = 10*phys.msPerMinute ;

    public Stamina() {}
    public Stamina (Stamina s) {
        this.currentStamina = s.currentStamina;
        this.penaltyEndTime = s.penaltyEndTime;
    }
    public Stamina (float s) {
        this.currentStamina = s;
    }
    float manoeuvreTimeFactor () {
        return 2f - 1.5f * currentStamina;
    }
    float weatherFactor(float windSpeed) {
        if ( windSpeed < 10  ) {
            return 1.0f + 0.02f * windSpeed ;
        }
        else if ( windSpeed < 20  ) {
            return 0.9f + 0.03f * windSpeed ;
        }
        else if ( windSpeed < 30  ) {
            return 0.5f + 0.05f * windSpeed ;
        }
        else return 2.0f;
    }

    float recovery(float windSpeed) { // minutes per percent
        if (windSpeed * phys.knots < 30)
            return (float) (10.0f - Math.cos(windSpeed * phys.knots * Math.PI / 30.0f ) * 5.0f);
        return 15;
    }

    public void recover(long timeDelta, float windSpeed) {
        currentStamina += (float) timeDelta / (float) phys.msPerMinute / recovery(windSpeed)  * 0.01f;
        if (currentStamina > 1.0f)
            currentStamina = 1.0f;
    }

    public void tackPenalty(Vector2 wind, long time) {
        long penaltyTime = time + tackPenalty(wind);
        if (penaltyTime > penaltyEndTime)
            penaltyEndTime = penaltyTime;
    }

    public void gybePenalty(Vector2 wind, long time) {
        long penaltyTime = time + gybePenalty(wind);
        if (penaltyTime > penaltyEndTime)
            penaltyEndTime = penaltyTime;
    }
    public void sailPenalty(Vector2 wind, long time) {
        long penaltyTime = time + sailPenalty(wind);
        if (penaltyTime > penaltyEndTime)
            penaltyEndTime = penaltyTime;
    }

    public float speedFactor(long time, long previousTime){
        if (penaltyEndTime <= previousTime)
            return weightFactor;
        if (penaltyEndTime > time)
            return speedFactor* weightFactor;
        long t = penaltyEndTime;
        float f = ((float) (t-previousTime) * speedFactor + (float)(time-t))/ (float) (time - previousTime);
        assert (f >= speedFactor && f <=1.0f);
        return f* weightFactor;
    }

    long tackPenalty(Vector2 wind) {
        float windSpeed = wind.mag() * phys.knots;
        float factor  =  manoeuvreTimeFactor();
        currentStamina -= tackEnergy * globalFactor * weatherFactor(windSpeed);
        if (currentStamina < 0.0f)
            currentStamina = 0.0f;
        if(windSpeed < 10)
            return tackPenalty10;
        if(windSpeed > 30)
            return tackPenalty30;
        return (long) ( factor *
                (tackPenalty10 + (tackPenalty30 - tackPenalty10) *(windSpeed -10) /20.0) );
    }

    long gybePenalty(Vector2 wind) {
        float windSpeed = wind.mag() * phys.knots;
        float factor  =  manoeuvreTimeFactor();
        currentStamina -= gybeEnergy * globalFactor * weatherFactor(windSpeed);
        if (currentStamina < 0.0f)
            currentStamina = 0.0f;
        if(windSpeed < 10)
            return gybePenalty10;
        if(windSpeed > 30)
            return gybePenalty30;
        return (long) ( factor *
                (gybePenalty10 + (gybePenalty30 - gybePenalty10) *(windSpeed -10) /20.0));
    }

    long sailPenalty(Vector2 wind) {
        float windSpeed = wind.mag() * phys.knots;
        float factor  =  manoeuvreTimeFactor();
        currentStamina -= sailEnergy * globalFactor * weatherFactor(windSpeed);
        if (currentStamina < 0.0f)
            currentStamina = 0.0f;
        if(windSpeed < 10)
            return sailPenalty10;
        if(windSpeed > 30)
            return sailPenalty30;
        return (long) ( factor  *
                sailPenalty10 + ((sailPenalty30 - sailPenalty10) *(windSpeed -10) /20.0) );
    }
}
