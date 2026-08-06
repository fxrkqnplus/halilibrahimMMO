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
 * head with a long snout and upright ears, a heavy shoulder ruff, long forearms
 * that hang past the knee, backward-jointed legs and a low tail. Proportions are
 * given in model units, sixteen to the block, in the same coordinate frame the
 * vanilla humanoid uses: the origin sits where a player's neck does, y grows
 * <em>downward</em>, and the feet land at y=24. Keeping that frame is what makes
 * the wolf stand on the ground the player is standing on without a single
 * correction at render time.</p>
 *
 * <h2>The UV layout is generated, not guessed - and that is the Faz 25 fix</h2>
 * <p>Faz 24 shipped this class against {@code tasarim/kurt-adam/texture.png}, a
 * finished 128x256 sheet supplied with no model file. A box-UV layout cannot be
 * recovered from an image - the islands abut, so any region could be any face -
 * so the {@code texOffs} calls were a layout this class <em>declared</em> and
 * hoped matched. It did not, and the result was the reported "bozuk" wolf: every
 * face showing a slice of some other part.</p>
 *
 * <p>Faz 25 reverses the direction. The table below is the single source of
 * truth, and {@code tools/kurt-adam-doku.py} paints
 * {@code assets/hmmo/textures/entity/kurt_adam.png} <em>to</em> it, using the
 * palette counted out of the original art. As long as the two tables agree the
 * model cannot come out wrong, and changing a part's size is a two-line edit in
 * two files rather than a repainting job.</p>
 *
 * <h2>Left and right share one island</h2>
 * <p>Every paired part ({@code kol}, {@code on-kol}, {@code pence},
 * {@code uyluk}, {@code baldir}, {@code ayak}, {@code kulak}) is built twice off
 * the same {@code texOffs}, the second with {@link CubeListBuilder#mirror()}.
 * That halves the sheet and makes the two sides symmetrical by construction
 * rather than by two people painting carefully.</p>
 */
public final class WerewolfModel extends EntityModel<AvatarRenderState> {

    /** The sheet these offsets are laid out on. Mirrored by {@code tools/kurt-adam-doku.py}. */
    public static final int TEXTURE_WIDTH = 128;
    public static final int TEXTURE_HEIGHT = 128;

    // --- Rest pose ----------------------------------------------------------
    /** The forward lean that makes the silhouette read as a predator rather than as a tall person. */
    private static final float BODY_LEAN = 0.28F;
    /** Where the hips sit, in model units below the neck. Chosen so the foot's underside lands on y=24. */
    private static final float HIP_Y = 12.5F;
    private static final float THIGH_LENGTH = 5.0F;
    private static final float SHIN_LENGTH = 5.0F;
    /** Rest angles of the three leg joints. Together they fold the leg into a wolf's crouch. */
    private static final float THIGH_REST = 0.60F;
    private static final float SHIN_REST = -1.10F;
    private static final float FOOT_REST = 0.50F;
    /** Rest angles of the arm, which hangs forward and slightly in. */
    private static final float ARM_REST = -0.22F;
    private static final float FOREARM_REST = 0.55F;
    private static final float TAIL_REST = 0.62F;

    // --- Animation ----------------------------------------------------------
    /** How far the limbs swing at a walk, in radians per unit of walk speed. */
    private static final float WALK_SWING = 1.05F;
    /** How much further they swing at a full sprint. */
    private static final float SPRINT_SWING = 0.75F;
    /** How fast the walk cycle reads the walk animation position. Vanilla's own constant. */
    private static final float STRIDE_RATE = 0.6662F;
    /** How far the whole body bobs on each step, in model units. */
    private static final float WALK_BOB = 0.9F;
    /** How far the head can turn to follow what the player is looking at, in degrees. */
    private static final float HEAD_YAW_LIMIT = 60.0F;
    private static final float HEAD_PITCH_LIMIT = 40.0F;
    private static final float DEGREES_TO_RADIANS = (float) (Math.PI / 180.0);
    /** How fast the chest breathes while standing still, in radians of phase per tick. */
    private static final float BREATH_RATE = 0.08F;
    /** How far the jaw opens at a full snarl, in radians. */
    private static final float JAW_OPEN = 0.45F;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart mane;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftForearm;
    private final ModelPart rightForearm;
    private final ModelPart leftThigh;
    private final ModelPart rightThigh;
    private final ModelPart leftShin;
    private final ModelPart rightShin;
    private final ModelPart leftFoot;
    private final ModelPart rightFoot;
    private final ModelPart tail;

    /**
     * What the wolf being drawn this frame is doing.
     *
     * <p>Written by {@link WerewolfLayer} immediately before {@code setupAnim},
     * because {@code EntityModel#setupAnim} takes only the render state and the
     * render state knows nothing about being airborne. One model instance draws
     * every wolf on screen, so this is scratch space for exactly one call and
     * never a memory of anything.</p>
     */
    private WolfMotion motion = WolfMotion.RESTING;

    public WerewolfModel(ModelPart root) {
        super(root);
        this.root = root;
        this.body = root.getChild("govde");
        this.mane = body.getChild("yele");
        this.head = body.getChild("kafa");
        this.jaw = head.getChild("alt-cene");
        this.leftEar = head.getChild("sol-kulak");
        this.rightEar = head.getChild("sag-kulak");
        this.leftArm = body.getChild("sol-kol");
        this.rightArm = body.getChild("sag-kol");
        this.leftForearm = leftArm.getChild("sol-on-kol");
        this.rightForearm = rightArm.getChild("sag-on-kol");
        this.tail = body.getChild("kuyruk");
        this.leftThigh = root.getChild("sol-bacak");
        this.rightThigh = root.getChild("sag-bacak");
        this.leftShin = leftThigh.getChild("sol-baldir");
        this.rightShin = rightThigh.getChild("sag-baldir");
        this.leftFoot = leftShin.getChild("sol-ayak");
        this.rightFoot = rightShin.getChild("sag-ayak");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // The chest, leaned forward. Everything except the legs hangs off it, so
        // the lean is applied once here rather than to each part in turn. Its
        // pivot sits a little forward of the neck so that leaning it does not
        // swing the hips out behind the legs.
        PartDefinition body = root.addOrReplaceChild("govde",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-6.0F, 0.0F, -5.0F, 12, 13, 9),
                PartPose.offsetAndRotation(0.0F, -1.0F, -1.0F, BODY_LEAN, 0.0F, 0.0F));

        // The shoulder ruff. It is the single part that most separates this
        // silhouette from a tall person: without it the chest is a box.
        body.addOrReplaceChild("yele",
                CubeListBuilder.create().texOffs(66, 40)
                        .addBox(-7.0F, -1.0F, -6.0F, 14, 6, 11),
                PartPose.offset(0.0F, 0.5F, 0.5F));

        PartDefinition head = body.addOrReplaceChild("kafa",
                CubeListBuilder.create().texOffs(44, 0)
                        .addBox(-4.0F, -8.0F, -5.0F, 8, 9, 8),
                PartPose.offsetAndRotation(0.0F, 0.5F, -2.0F, -BODY_LEAN, 0.0F, 0.0F));
        head.addOrReplaceChild("burun",
                CubeListBuilder.create().texOffs(0, 24)
                        .addBox(-2.5F, 0.0F, -6.0F, 5, 4, 7),
                PartPose.offset(0.0F, -4.0F, -4.0F));
        // The jaw is its own part because it is the one piece of this model that
        // moves for a reason other than walking: it opens when the wolf runs and
        // hangs open at a sprint.
        head.addOrReplaceChild("alt-cene",
                CubeListBuilder.create().texOffs(26, 24)
                        .addBox(-2.5F, 0.0F, -6.0F, 5, 2, 6),
                PartPose.offset(0.0F, 0.0F, -4.0F));
        head.addOrReplaceChild("sol-kulak",
                CubeListBuilder.create().texOffs(50, 24)
                        .addBox(-1.5F, -4.0F, -1.0F, 3, 4, 2),
                PartPose.offsetAndRotation(-2.5F, -8.0F, -0.5F, -0.15F, 0.0F, -0.30F));
        head.addOrReplaceChild("sag-kulak",
                CubeListBuilder.create().texOffs(50, 24).mirror()
                        .addBox(-1.5F, -4.0F, -1.0F, 3, 4, 2),
                PartPose.offsetAndRotation(2.5F, -8.0F, -0.5F, -0.15F, 0.0F, 0.30F));

        // Long forearms. They hang past the knee in both reference views, which
        // is most of what separates this silhouette from a tall humanoid.
        PartDefinition leftArm = body.addOrReplaceChild("sol-kol",
                CubeListBuilder.create().texOffs(62, 24)
                        .addBox(-2.0F, 0.0F, -2.5F, 4, 8, 5),
                PartPose.offsetAndRotation(-6.5F, 1.0F, -0.5F, ARM_REST, 0.0F, 0.10F));
        PartDefinition leftForearm = leftArm.addOrReplaceChild("sol-on-kol",
                CubeListBuilder.create().texOffs(82, 24)
                        .addBox(-2.0F, 0.0F, -2.0F, 4, 8, 4),
                PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, FOREARM_REST, 0.0F, 0.0F));
        leftForearm.addOrReplaceChild("sol-pence",
                CubeListBuilder.create().texOffs(100, 24)
                        .addBox(-2.5F, 0.0F, -4.5F, 5, 3, 6),
                PartPose.offset(0.0F, 8.0F, 0.0F));

        PartDefinition rightArm = body.addOrReplaceChild("sag-kol",
                CubeListBuilder.create().texOffs(62, 24).mirror()
                        .addBox(-2.0F, 0.0F, -2.5F, 4, 8, 5),
                PartPose.offsetAndRotation(6.5F, 1.0F, -0.5F, ARM_REST, 0.0F, -0.10F));
        PartDefinition rightForearm = rightArm.addOrReplaceChild("sag-on-kol",
                CubeListBuilder.create().texOffs(82, 24).mirror()
                        .addBox(-2.0F, 0.0F, -2.0F, 4, 8, 4),
                PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, FOREARM_REST, 0.0F, 0.0F));
        rightForearm.addOrReplaceChild("sag-pence",
                CubeListBuilder.create().texOffs(100, 24).mirror()
                        .addBox(-2.5F, 0.0F, -4.5F, 5, 3, 6),
                PartPose.offset(0.0F, 8.0F, 0.0F));

        body.addOrReplaceChild("kuyruk",
                CubeListBuilder.create().texOffs(78, 0)
                        .addBox(-2.0F, 0.0F, 0.0F, 4, 4, 11),
                PartPose.offsetAndRotation(0.0F, 11.5F, 3.5F, TAIL_REST, 0.0F, 0.0F));

        // Legs hang off the root rather than off the leaning chest: a leg that
        // leaned with the body would put the feet behind the player's own
        // position and the wolf would look like it was falling over backwards.
        addLeg(root, "sol", -3.5F, false);
        addLeg(root, "sag", 3.5F, true);

        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    /**
     * One digitigrade leg: thigh back, shin forward, foot flat.
     *
     * <p>The three rest angles are not decoration. Thigh {@value #THIGH_REST}
     * back and shin {@value #SHIN_REST} forward put the ankle at roughly
     * {@code HIP_Y + THIGH_LENGTH*cos + SHIN_LENGTH*cos}, which lands the foot's
     * underside on y=24 - the ground the player is standing on. Change any one
     * of them and the wolf floats or sinks.</p>
     */
    private static void addLeg(PartDefinition root, String side, float x, boolean mirror) {
        PartDefinition thigh = root.addOrReplaceChild(side + "-bacak",
                CubeListBuilder.create().texOffs(0, 40).mirror(mirror)
                        .addBox(-2.5F, 0.0F, -3.0F, 5, 5, 6),
                PartPose.offsetAndRotation(x, HIP_Y, 0.0F, THIGH_REST, 0.0F, 0.0F));
        PartDefinition shin = thigh.addOrReplaceChild(side + "-baldir",
                CubeListBuilder.create().texOffs(24, 40).mirror(mirror)
                        .addBox(-2.0F, 0.0F, -2.0F, 4, 5, 4),
                PartPose.offsetAndRotation(0.0F, THIGH_LENGTH, 0.0F, SHIN_REST, 0.0F, 0.0F));
        shin.addOrReplaceChild(side + "-ayak",
                CubeListBuilder.create().texOffs(42, 40).mirror(mirror)
                        .addBox(-2.0F, 0.0F, -5.0F, 4, 3, 7),
                PartPose.offsetAndRotation(0.0F, SHIN_LENGTH, 0.0F, FOOT_REST, 0.0F, 0.0F));
    }

    /** Told what this particular wolf is doing, one frame before it is drawn. */
    void motion(WolfMotion motion) {
        this.motion = motion;
    }

    /**
     * The whole animation set, driven by the render state the game already
     * computed for the player being replaced plus the eased motion weights
     * {@link WolfFormTicker} keeps.
     *
     * <p>Nothing here asks the server anything, and nothing here is a state
     * machine. Every pose is written as an offset from the rest pose and then
     * <em>weighted in</em> by one of {@link WolfMotion}'s numbers, so walking,
     * sprinting, leaping, falling, diving and crouching can all be part-way on at
     * once - which is what a body actually does. A wolf that jumps while running
     * is both, in proportion.</p>
     */
    @Override
    public void setupAnim(AvatarRenderState state) {
        // Puts every part back on its rest angle - Model#setupAnim is exactly
        // one call to resetPose(). It matters here more than in most models,
        // because one instance draws every wolf on screen and a frame that
        // inherited the previous entity's pose would blend two animals.
        super.setupAnim(state);

        float swing = state.walkAnimationPos;
        float speed = Math.min(1.0F, state.walkAnimationSpeed);
        float sprint = clamp01(motion.sprint());
        float air = clamp01(motion.air());
        float slam = clamp01(motion.slam());
        float crouch = clamp01(motion.crouch());
        float ground = 1.0F - air;

        // The head turns with the difference between where the player is looking
        // and where their body points; yRot alone would spin the head by the
        // whole world heading and take it off the shoulders.
        head.yRot = clampDegrees(state.yRot - state.bodyRot, HEAD_YAW_LIMIT) * DEGREES_TO_RADIANS;
        head.xRot = clampDegrees(state.xRot, HEAD_PITCH_LIMIT) * DEGREES_TO_RADIANS - BODY_LEAN;

        idle(state.ageInTicks, speed);
        walk(swing, speed, sprint, ground);
        if (air > 0.001F) {
            airborne(air, clamp01(motion.rise()), Math.max(0.0F, -motion.rise()), slam);
        }
        if (crouch > 0.001F) {
            crouch(crouch);
        }
        // The jaw hangs open the harder the wolf is working, and all the way open
        // during a dive. A closed mouth on a sprinting predator is the one thing
        // that makes the whole form read as a costume.
        jaw.xRot += JAW_OPEN * Math.max(sprint * speed, slam);
    }

    /**
     * Standing still: a chest that breathes and a tail that never quite stops.
     *
     * <p>A perfectly still tail is the single thing that makes a model read as a
     * statue rather than as an animal, so it keeps a slow sweep even at rest and
     * only tightens as the wolf starts moving.</p>
     */
    private void idle(float ageInTicks, float speed) {
        float rest = 1.0F - speed;
        float breath = (float) Math.sin(ageInTicks * BREATH_RATE);
        body.xRot += breath * 0.030F * rest;
        mane.xRot += breath * 0.045F * rest;
        head.xRot += breath * 0.020F * rest;
        tail.yRot += (float) Math.cos(ageInTicks * BREATH_RATE * 0.75F) * 0.30F * (0.35F + speed);
        leftEar.zRot += breath * 0.10F * rest;
        rightEar.zRot -= breath * 0.10F * rest;
    }

    /**
     * The walk and the run, which are one cycle with two amplitudes.
     *
     * <p>Arms and legs swing in opposite pairs, the whole body bobs twice per
     * stride, and the sprint weight both widens the swing and drags the chest
     * further forward - a wolf at a run is nearly horizontal.</p>
     */
    private void walk(float swing, float speed, float sprint, float ground) {
        float amplitude = (WALK_SWING + SPRINT_SWING * sprint) * speed * ground;
        if (amplitude <= 0.001F) {
            return;
        }
        float stride = (float) Math.cos(swing * STRIDE_RATE) * amplitude;
        float counter = (float) Math.cos(swing * STRIDE_RATE + Math.PI) * amplitude;

        leftArm.xRot += stride;
        rightArm.xRot += counter;
        // The forearm trails the upper arm by a quarter cycle, which is what
        // turns a swinging stick into an elbow.
        leftForearm.xRot += Math.max(0.0F, (float) Math.cos(swing * STRIDE_RATE - 1.2F)) * amplitude * 0.55F;
        rightForearm.xRot += Math.max(0.0F, (float) Math.cos(swing * STRIDE_RATE + Math.PI - 1.2F)) * amplitude * 0.55F;

        leftThigh.xRot += counter * 0.85F;
        rightThigh.xRot += stride * 0.85F;
        // The shin folds only on the way through, never on the way out: a knee
        // that bends both ways is the classic broken walk cycle.
        leftShin.xRot -= Math.max(0.0F, counter) * 0.9F;
        rightShin.xRot -= Math.max(0.0F, stride) * 0.9F;
        leftFoot.xRot += Math.max(0.0F, counter) * 0.5F;
        rightFoot.xRot += Math.max(0.0F, stride) * 0.5F;

        // Twice per stride, because both feet land in one cycle.
        root.y += Math.abs((float) Math.sin(swing * STRIDE_RATE)) * WALK_BOB * speed;
        body.xRot += 0.22F * sprint * speed;
        head.xRot -= 0.18F * sprint * speed;
        tail.xRot -= 0.35F * sprint * speed;
    }

    /**
     * Off the ground: a leap, a fall and a dive, mixed by weight.
     *
     * <p>The three are genuinely different shapes and all three can be part-way
     * on at once - the top of a jump is a little of the first and a little of the
     * second, which is exactly when a hard-switched animation snaps.</p>
     *
     * @param air   how far off the ground the pose is blended in at all
     * @param climb 0..1, how hard the wolf is still going up
     * @param drop  0..1, how hard it is coming down
     * @param slam  0..1, the crouch-held ground dive
     */
    private void airborne(float air, float climb, float drop, float slam) {
        // Leap: legs tucked under, arms thrown back, tail up, body arched.
        float leap = climb * air;
        leftThigh.xRot -= 0.85F * leap;
        rightThigh.xRot -= 0.85F * leap;
        leftShin.xRot -= 0.70F * leap;
        rightShin.xRot -= 0.70F * leap;
        leftArm.xRot += 0.90F * leap;
        rightArm.xRot += 0.90F * leap;
        leftForearm.xRot += 0.40F * leap;
        rightForearm.xRot += 0.40F * leap;
        tail.xRot -= 0.75F * leap;
        body.xRot -= 0.20F * leap;

        // Fall: legs reach for the ground, arms out to the sides, tail streaming.
        float fall = drop * air * (1.0F - slam);
        leftThigh.xRot += 0.55F * fall;
        rightThigh.xRot += 0.55F * fall;
        leftShin.xRot += 0.65F * fall;
        rightShin.xRot += 0.65F * fall;
        leftArm.xRot -= 0.55F * fall;
        rightArm.xRot -= 0.55F * fall;
        leftArm.zRot -= 0.45F * fall;
        rightArm.zRot += 0.45F * fall;
        leftForearm.xRot -= 0.30F * fall;
        rightForearm.xRot -= 0.30F * fall;
        tail.xRot += 0.35F * fall;
        head.xRot += 0.25F * fall;

        // Dive: everything points down, hard. This is the pose the ground slam
        // is read from, so it deliberately looks like nothing else the wolf does.
        leftThigh.xRot -= 0.45F * slam;
        rightThigh.xRot -= 0.45F * slam;
        leftShin.xRot += 1.10F * slam;
        rightShin.xRot += 1.10F * slam;
        leftArm.xRot += 2.10F * slam;
        rightArm.xRot += 2.10F * slam;
        leftArm.zRot += 0.25F * slam;
        rightArm.zRot -= 0.25F * slam;
        leftForearm.xRot -= 0.80F * slam;
        rightForearm.xRot -= 0.80F * slam;
        body.xRot += 0.55F * slam;
        head.xRot += 0.35F * slam;
        tail.xRot -= 0.90F * slam;
        mane.xRot -= 0.20F * slam;
    }

    /** Sneaking: the whole animal drops and the head comes forward, which is a wolf stalking. */
    private void crouch(float crouch) {
        root.y += 2.6F * crouch;
        body.xRot += 0.40F * crouch;
        head.xRot -= 0.30F * crouch;
        leftThigh.xRot += 0.35F * crouch;
        rightThigh.xRot += 0.35F * crouch;
        leftShin.xRot -= 0.55F * crouch;
        rightShin.xRot -= 0.55F * crouch;
        leftFoot.xRot += 0.20F * crouch;
        rightFoot.xRot += 0.20F * crouch;
        tail.xRot += 0.35F * crouch;
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    /** Clamps a look angle so the head turns rather than snapping round on its neck. */
    private static float clampDegrees(float degrees, float limit) {
        return Math.max(-limit, Math.min(limit, degrees));
    }
}
