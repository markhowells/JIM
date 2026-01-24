/**
 * Web-based UI components for JIM.
 *
 * This package provides a browser-based interface to the JIM routing engine:
 *
 * - {@link uk.co.sexeys.ui.web.WebRenderer} - Renderer implementation that serializes
 *   drawing commands to JSON for transmission to browser clients
 *
 * - {@link uk.co.sexeys.ui.web.DrawCommand} - Serializable drawing command that maps
 *   to HTML5 Canvas operations
 *
 * - {@link uk.co.sexeys.ui.web.MapController} - REST controller exposing endpoints for
 *   map rendering, pan/zoom, time control, and layer toggling
 *
 * - {@link uk.co.sexeys.ui.web.MapService} - Service managing map state and coordinating
 *   rendering operations
 *
 * To start the web server, run {@link uk.co.sexeys.WebApplication}.
 * The interface will be available at http://localhost:8080
 */
package uk.co.sexeys.ui.web;
