package com.stagelite.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

/**
 * Haz de luz: igual que RenderType.lightning() (aditivo, sin textura) pero SIN escribir profundidad.
 * Con lightning() el haz tapaba a los jugadores que estaban dentro de el.
 */
public final class ModRenderTypes extends RenderType {
    public static final RenderType BEAM = create("stagelite_beam",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, false,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    private ModRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                           boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
    }
}
