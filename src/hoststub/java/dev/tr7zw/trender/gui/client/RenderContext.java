package dev.tr7zw.trender.gui.client;

/**
 * Compile-time-only shim for tr7zw's TRender type, which is nested inside the PaperDoll jar and
 * therefore not available as a build dependency. The real class is supplied by PaperDoll at
 * runtime; this source set is never packaged into the mod jar. Only the type name matters because
 * Mixin matches handler descriptors by name.
 */
public class RenderContext {
}
