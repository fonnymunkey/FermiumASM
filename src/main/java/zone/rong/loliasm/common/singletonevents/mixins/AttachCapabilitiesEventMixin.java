package zone.rong.loliasm.common.singletonevents.mixins;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import org.spongepowered.asm.mixin.*;
import zone.rong.loliasm.common.singletonevents.IRefreshEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

@Mixin(value = AttachCapabilitiesEvent.class, remap = false)
public class AttachCapabilitiesEventMixin<T> extends Event implements IRefreshEvent {

    @Shadow @Final @Mutable private T obj;
    @Shadow @Final @Mutable private Map<ResourceLocation, ICapabilityProvider> caps = new Object2ObjectArrayMap<>(1);

    @Unique private EventPriority loliPriority = null;

    @Override
    public void refresh(Object... data) {
        this.obj = (T) data[0];
        this.caps.clear();
        this.loliPriority = null;
    }

    @Nullable
    @Override
    public EventPriority getPhase() {
        return loliPriority;
    }

    @Override
    public void setPhase(@Nonnull EventPriority next) {
        IRefreshEvent.checkPrecondition(this.loliPriority, next);
        this.loliPriority = next;
    }

}
