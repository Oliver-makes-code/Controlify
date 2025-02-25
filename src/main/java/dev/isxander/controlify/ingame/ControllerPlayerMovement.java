package dev.isxander.controlify.ingame;

import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.controller.Controller;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;

public class ControllerPlayerMovement extends Input {
    private final Controller<?, ?> controller;
    private final LocalPlayer player;
    private boolean wasFlying, wasPassenger;

    public ControllerPlayerMovement(Controller<?, ?> controller, LocalPlayer player) {
        this.controller = controller;
        this.player = player;
    }

    @Override
    public void tick(boolean slowDown, float movementMultiplier) {
        if (Minecraft.getInstance().screen != null || player == null) {
            this.up = false;
            this.down = false;
            this.left = false;
            this.right = false;
            this.leftImpulse = 0;
            this.forwardImpulse = 0;
            this.jumping = false;
            this.shiftKeyDown = false;
            return;
        }

        var bindings = controller.bindings();

        this.forwardImpulse = bindings.WALK_FORWARD.state() - bindings.WALK_BACKWARD.state();
        this.leftImpulse = bindings.WALK_LEFT.state() - bindings.WALK_RIGHT.state();

        if (Controlify.instance().config().globalSettings().shouldUseKeyboardMovement()) {
            float threshold = controller.config().buttonActivationThreshold;

            this.forwardImpulse = Math.abs(this.forwardImpulse) >= threshold ? Math.copySign(1, this.forwardImpulse) : 0;
            this.leftImpulse = Math.abs(this.leftImpulse) >= threshold ? Math.copySign(1, this.leftImpulse) : 0;
        }

        this.up = this.forwardImpulse > 0;
        this.down = this.forwardImpulse < 0;
        this.left = this.leftImpulse > 0;
        this.right = this.leftImpulse < 0;

        if (slowDown) {
            this.leftImpulse *= movementMultiplier;
            this.forwardImpulse *= movementMultiplier;
        }

        // this over-complication is so exiting a GUI with the button still held doesn't trigger a jump.
        if (bindings.JUMP.justPressed())
            this.jumping = true;
        if (!bindings.JUMP.held())
            this.jumping = false;

        if (player.getAbilities().flying || (player.isInWater() && !player.onGround()) || player.getVehicle() != null || !controller.config().toggleSneak) {
            if (bindings.SNEAK.justPressed())
                this.shiftKeyDown = true;
            if (!bindings.SNEAK.held())
                this.shiftKeyDown = false;
        } else {
            if (bindings.SNEAK.justPressed()) {
                this.shiftKeyDown = !this.shiftKeyDown;
            }
        }
        if ((!player.getAbilities().flying && wasFlying && player.onGround()) || (!player.isPassenger() && wasPassenger)) {
            this.shiftKeyDown = false;
        }

        this.wasFlying = player.getAbilities().flying;
        this.wasPassenger = player.isPassenger();
    }

    public static void updatePlayerInput(@Nullable LocalPlayer player) {
        if (player == null)
            return;

        if (Controlify.instance().getCurrentController().isPresent() && Controlify.instance().currentInputMode().isController()) {
            player.input = new DualInput(
                    new KeyboardInput(Minecraft.getInstance().options),
                    new ControllerPlayerMovement(Controlify.instance().getCurrentController().get(), player)
            );
        } else if (!(player.input instanceof KeyboardInput)) {
            player.input = new KeyboardInput(Minecraft.getInstance().options);
        }

    }
}
