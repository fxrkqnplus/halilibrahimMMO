package com.halilibrahim.halilibrahimmmo.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * The body Asil Kurt puts on: a hunched, digitigrade wolf that stands where the
 * player stands.
 *
 * <p>Built from the two reference renders in {@code tasarim/kurt-adam} - a wolf
 * head with a long snout and upright ears, a heavy chest, long forearms that
 * hang past the knee, backward-jointed legs and a low tail. Proportions are
 * given in model units, sixteen to the block, in the same coordinate frame the
 * vanilla humanoid uses: the origin sits where a player's neck does, y grows
 * <em>downward</em>, and the feet land at y=24. Keeping that frame is what makes
 * the wolf stand on the ground the player is standing on without a single
 * correction at render time.</p>
 *
 * <h2>The UV layout is ours, and this is the one thing to check first</h2>
 * <p>{@code texture.png} was supplied as a finished 128x256 sheet with no
 * transparent gutters, so the islands it was painted against cannot be
 * recovered from the image - every pixel is opaque and any region could be any
 * face. The {@code texOffs} calls below are therefore a layout <em>this class
 * defines</em>, not one read off the art: they are packed row by row from the
 * top left, largest part first, in the order head, snout, ears, chest, arms,
 * legs, tail.</p>
 *
 * <p>If the sheet came out of a Blockbench project, its own model file carries
 * the real offsets and dropping them into this one block is the whole fix -
 * nothing else in the transformation depends on them. Every offset lives here
 * and nowhere else precisely so that stays true.</p>
 */
public final class WerewolfModel extends EntityModel<AvatarRenderState> {

    /** The sheet these offsets are laid out on. */
    public static final int TEXTURE_WIDTH = 128;
    public static final int TEXTURE_HEIGHT = 256;

    /** How far the arms and legs swing, in radians per unit of walk speed. */
    private static final float LIMB_SWING_SCALE = 0.9F;
    /** The forward lean that makes the silhouette read as a predator rather than as a tall person. */
    private static final float BODY_LEAN = 0.18F;
    /** How far the head can turn to follow what the player is looking at, in degrees. */
    private static final float HEAD_YAW_LIMIT = 60.0F;
    private static final float HEAD_PITCH_LIMIT = 40.0F;
    private static final float DEGREES_TO_RADIANS = (float) (Math.PI / 180.0);

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart tail;

    public WerewolfModel(ModelPart root) {
        super(root);
        this.body = root.getChild("govde");
        this.head = body.getChild("kafa");
        this.leftArm = body.getChild("sol-kol");
        this.rightArm = body.getChild("sag-kol");
        this.leftLeg = root.getChild("sol-bacak");
        this.rightLeg = root.getChild("sag-bacak");
        this.tail = body.getChild("kuyruk");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // The chest, leaned forward. Everything except the legs hangs off it, so
        // the lean is applied once here rather than to each part in turn.
        PartDefinition body = root.addOrReplaceChild("govde",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-6.0F, 0.0F, -4.0F, 12, 15, 8),
                PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, BODY_LEAN, 0.0F, 0.0F));

        PartDefinition head = body.addOrReplaceChild("kafa",
                CubeListBuilder.create().texOffs(0, 24)
                        .addBox(-4.5F, -9.0F, -5.0F, 9, 9, 9),
                PartPose.offsetAndRotation(0.0F, 0.0F, -1.0F, -BODY_LEAN, 0.0F, 0.0F));
        head.addOrReplaceChild("burun",
                CubeListBuilder.create().texOffs(38, 24)
                        .addBox(-2.5F, -5.5F, -11.0F, 5, 5, 6),
                PartPose.ZERO);
        head.addOrReplaceChild("sol-kulak",
                CubeListBuilder.create().texOffs(60, 24)
                        .addBox(-4.5F, -14.0F, -2.0F, 3, 5, 2),
                PartPose.ZERO);
        head.addOrReplaceChild("sag-kulak",
                CubeListBuilder.create().texOffs(72, 24)
                        .addBox(1.5F, -14.0F, -2.0F, 3, 5, 2),
                PartPose.ZERO);

        // Long forearms. They hang past the knee in both reference views, which
        // is most of what separates this silhouette from a tall humanoid.
        body.addOrReplaceChild("sol-kol",
                CubeListBuilder.create().texOffs(0, 44)
                        .addBox(-4.0F, -1.0F, -3.0F, 5, 18, 6),
                PartPose.offset(-7.0F, 2.0F, 0.0F));
        body.addOrReplaceChild("sag-kol",
                CubeListBuilder.create().texOffs(24, 44)
                        .addBox(-1.0F, -1.0F, -3.0F, 5, 18, 6),
                PartPose.offset(7.0F, 2.0F, 0.0F));

        body.addOrReplaceChild("kuyruk",
                CubeListBuilder.create().texOffs(48, 44)
                        .addBox(-2.0F, 0.0F, 0.0F, 4, 4, 11),
                PartPose.offsetAndRotation(0.0F, 11.0F, 4.0F, 0.6F, 0.0F, 0.0F));

        // Legs hang off the root rather than the leaning chest: a leg that
        // leaned with the body would put the feet behind the player's own
        // position and the wolf would look like it was falling over backwards.
        root.addOrReplaceChild("sol-bacak",
                CubeListBuilder.create().texOffs(0, 70)
                        .addBox(-3.0F, 0.0F, -3.5F, 6, 13, 7),
                PartPose.offset(-3.5F, 11.0F, 0.0F));
        root.addOrReplaceChild("sag-bacak",
                CubeListBuilder.create().texOffs(28, 70)
                        .addBox(-3.0F, 0.0F, -3.5F, 6, 13, 7),
                PartPose.offset(3.5F, 11.0F, 0.0F));

        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    /**
     * Walk cycle and head tracking, driven entirely by the render state the game
     * already computed for the player it is replacing.
     *
     * <p>Nothing here asks the server anything. The wolf swings its arms because
     * the player under it is walking, and looks where they are looking, so a
     * transformed player who is standing still is a wolf standing still.</p>
     */
    @Override
    public void setupAnim(AvatarRenderState state) {
        super.setupAnim(state);
        float swing = state.walkAnimationPos;
        float speed = Math.min(1.0F, state.walkAnimationSpeed);

        // yRot is where the head points and bodyRot is where the body points;
        // the head's own turn is the difference. Using yRot alone would turn the
        // head by the whole world-space heading and spin it off the shoulders.
        head.yRot = clampDegrees(state.yRot - state.bodyRot, HEAD_YAW_LIMIT) * DEGREES_TO_RADIANS;
        head.xRot = clampDegrees(state.xRot, HEAD_PITCH_LIMIT) * DEGREES_TO_RADIANS - BODY_LEAN;

        float stride = (float) Math.cos(swing * 0.6662F) * LIMB_SWING_SCALE * speed;
        leftArm.xRot = stride;
        rightArm.xRot = -stride;
        leftLeg.xRot = -stride;
        rightLeg.xRot = stride;
        // The tail sweeps at half the stride and never stops entirely: a
        // perfectly still tail is the single thing that makes a model read as a
        // statue rather than as an animal.
        tail.yRot = (float) Math.cos(swing * 0.3331F) * 0.35F * (0.35F + speed);
    }

    /** Clamps a look angle so the head turns rather than snapping round on its neck. */
    private static float clampDegrees(float degrees, float limit) {
        return Math.max(-limit, Math.min(limit, degrees));
    }
}
