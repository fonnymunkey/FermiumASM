package mirror.normalasm.core;

import mirror.normalasm.config.NormalConfig;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import mirror.normalasm.NormalLogger;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class NormalSpriteMixinPlugin implements IMixinConfigPlugin {

    private Boolean shouldApply;

    @Override
    public void onLoad(String s) { }

    @Override
    public String getRefMapperConfig() {
        return "";
    }

    @Override
    public boolean shouldApplyMixin(String s, String s1) {
        if (this.shouldApply != null) {
            return this.shouldApply;
        }
        this.shouldApply = NormalConfig.instance.onDemandAnimatedTextures;
        
        if (this.shouldApply) {
            if (NormalTransformer.isOptifineInstalled) {
                this.shouldApply = false;
                NormalLogger.instance.error("Optifine is installed. On demand sprites won't be activated as Optifine already has Smart Animations.");
            }
        }
        return this.shouldApply;
    }

    @Override
    public void acceptTargets(Set<String> set, Set<String> set1) { }

    @Override
    public List<String> getMixins() {
        return Collections.emptyList();
    }

    @Override
    public void preApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) { }

    @Override
    public void postApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) { }

}
