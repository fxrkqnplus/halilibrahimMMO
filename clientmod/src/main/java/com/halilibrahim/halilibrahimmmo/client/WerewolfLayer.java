package com.halilibrahim.halilibrahimmmo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.resources.Identifier;

/**
 * Draws {@link WerewolfModel} over a transformed player.
 *
 * <p><strong>A layer, not a replacement, and that is a deliberate choice about
 * risk rather than a shortcut.</strong> Genuinely swapping the model a player
 * renderer uses means a mixin into {@code AvatarRenderer}, which is build
 * machinery and a rename away from breaking on every game update - the same
 * reasoning §22.17-19 used when it refused to widen access just to read a
 * panel's position. Fabric's own render-layer hook is public API, and layers are
 * drawn for an invisible entity: {@code AvatarRenderer.shouldRenderLayers}
 * returns {@code !isSpectator} and nothing else, which was read out of the
 * compiled class rather than assumed.</p>
 *
 * <p>So the transformation is two halves that only work together:
 * {@link WolfFormTicker} sets the client's own copy of the player invisible, and
 * this draws the wolf where the body used to be. If either half were to fail the
 * other is harmless - a plain player, or a wolf standing in a player - and
 * neither can affect what anybody's ability actually does.</p>
 */
public final class WerewolfLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(HudProtocol.NAMESPACE, "textures/entity/kurt_adam.png");

    /** Untinted. The sheet already carries every colour the form has. */
    private static final int NO_TINT = 0xFFFFFFFF;
    /**
     * Where in the render order the wolf is submitted.
     *
     * <p>Zero is the same order the vanilla body would have used, which is what
     * puts the wolf exactly where the player was rather than in front of or
     * behind everything else in the scene.</p>
     */
    private static final int RENDER_ORDER = 0;

    private final WerewolfModel model;
    private final WolfFormState forms;

    public WerewolfLayer(
            RenderLayerParent<AvatarRenderState, PlayerModel> parent, ModelPart bakedRoot, WolfFormState forms
    ) {
        super(parent);
        this.model = new WerewolfModel(bakedRoot);
        this.forms = forms;
    }

    @Override
    public void submit(
            PoseStack pose, SubmitNodeCollector collector, int packedLight,
            AvatarRenderState state, float yRot, float xRot
    ) {
        // Matched on the entity id the render state already carries. A render
        // state has no UUID, and resolving one here would be a level scan per
        // transformed player per frame - WolfFormTicker does it once a tick.
        if (forms.isEmpty() || !forms.isTransformedEntity(state.id)) {
            return;
        }
        // Handed over immediately before the pose is built, because the render
        // state has no notion of being airborne and one model instance draws
        // every wolf on screen - see WerewolfModel#motion.
        model.motion(forms.motionOf(state.id));
        model.setupAnim(state);
        renderColoredCutoutModel(model, TEXTURE, pose, collector, packedLight, state, NO_TINT, RENDER_ORDER);
    }
}
