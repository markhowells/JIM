package uk.co.sexeys.CMap;

import java.io.*;
import java.util.*;

public class GeometryObject {

    public final CIB cib;

    public int n_attributes = 0;
    public byte[] attributes_block;

    public int object_type;
    public int geotype;

    public int n_geom_elements;
    public List<Integer> pGeometry;   // For vector or multi-element geometry
    public int segment_usage;

    public int n_related_objects;
    public List<Integer> p_related_object_pointer_array;

    public GeometryObject(DataInputStream f, CIB cib) throws IOException {
        this.cib = cib;
        this.attributes_block = new byte[0];

        this.object_type = DecodeTables.readByte(f);
        this.geotype = DecodeTables.readByte(f);
        this.pGeometry = new ArrayList<>();

        int obj_desc_bytes = DecodeTables.readShort(f) - 4;

        int geomTypeLow = (geotype & 0x0F);

        // ----------------------------------------------------
        // GEOMETRY TYPES
        // ----------------------------------------------------
        if (geomTypeLow == 4 || geomTypeLow == 2) {
            this.n_geom_elements = DecodeTables.readShort(f);
            int t = (this.n_geom_elements * 2) + 2;
            obj_desc_bytes -= t;


            for (int i = 0; i < this.n_geom_elements; i++) {
                int index = DecodeTables.readShort(f);

                if ((index & 0x1FFF) > cib.m_nvector_records) {
                    throw new IOException("Invalid vector index");
                }

                pGeometry.add(index);
                segment_usage = (index >> 13) & 0xFF;
            }

        } else if (geomTypeLow == 1) {
            int index = DecodeTables.readShort(f);
            obj_desc_bytes -= 2;
            pGeometry.add(index);

        } else if (geomTypeLow == 8) {
            int index = DecodeTables.readShort(f);
            obj_desc_bytes -= 2;
            pGeometry.add(index);

        } else {
            // Unsupported - skip bytes
            DecodeTables.readBytes(f, obj_desc_bytes);
            return;
        }

        // ----------------------------------------------------
        // RELATED OBJECTS (bit 0x10)
        // ----------------------------------------------------
        if ((geotype & 0x10) == 0x10) {
            this.n_related_objects = DecodeTables.readByte(f);
            int t = (this.n_related_objects * 2) + 1;
            obj_desc_bytes -= t;

            this.p_related_object_pointer_array = new ArrayList<>();

            for (int i = 0; i < this.n_related_objects; i++) {
                int index = DecodeTables.readShort(f);
                p_related_object_pointer_array.add(index);
            }
        }

        // Related object count as 16-bit integer (bit 0x20)
        if ((geotype & 0x20) == 0x20) {
            this.n_related_objects = DecodeTables.readShort(f);
            obj_desc_bytes -= 2;
        }

        // ----------------------------------------------------
        // ATTRIBUTES (bit 0x80)
        // ----------------------------------------------------
        if ((geotype & 0x80) == 0x80) {
            this.n_attributes = DecodeTables.readByte(f);
            obj_desc_bytes -= 1;

            this.attributes_block = DecodeTables.readBytes(f, obj_desc_bytes);
            obj_desc_bytes = 0;
        }

        if (obj_desc_bytes != 0) {
            System.out.println("Warning: Did not read all bytes in Geometry_Object");
        }
    }
}
