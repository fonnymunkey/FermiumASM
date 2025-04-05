package mirror.normalasm.core;

import fermiumbooter.FermiumRegistryAPI;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import mirror.normalasm.UnsafeNormal;
import mirror.normalasm.config.NormalConfig;
import mirror.normalasm.NormalLogger;
import mirror.normalasm.api.DeobfuscatingRewritePolicy;
import mirror.normalasm.api.StacktraceDeobfuscator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@IFMLLoadingPlugin.Name("FermiumASM")
@IFMLLoadingPlugin.MCVersion(ForgeVersion.mcVersion)
public class NormalLoadingPlugin implements IFMLLoadingPlugin {

    public static final String VERSION = "5.24";

    public static final boolean isDeobf = FMLLaunchHandler.isDeobfuscatedEnvironment();

    // public static final boolean isModDirectorInstalled = NormalReflector.doesTweakExist("net.jan.moddirector.launchwrapper.ModDirectorTweaker");
    public static final boolean isVMOpenJ9 = SystemUtils.JAVA_VM_NAME.toLowerCase(Locale.ROOT).contains("openj9");
    public static final boolean isClient = FMLLaunchHandler.side() == Side.CLIENT;

    public NormalLoadingPlugin() {
        NormalLogger.instance.info("FermiumASM is on the {}-side.", isClient ? "client" : "server");
        NormalLogger.instance.info("FermiumASM is preparing and loading in mixins");
        if (NormalConfig.instance.outdatedCaCertsFix) {
            try (InputStream is = this.getClass().getResource("/cacerts").openStream()) {
                File cacertsCopy = File.createTempFile("cacerts", "");
                cacertsCopy.deleteOnExit();
                FileUtils.copyInputStreamToFile(is, cacertsCopy);
                System.setProperty("javax.net.ssl.trustStore", cacertsCopy.getAbsolutePath());
                NormalLogger.instance.warn("Replacing CA Certs with an updated one...");
            } catch (Exception e) {
                NormalLogger.instance.warn("Unable to replace CA Certs.", e);
            }
        }
        if (NormalConfig.instance.removeForgeSecurityManager) {
            UnsafeNormal.removeFMLSecurityManager();
        }
        if (NormalConfig.instance.crashReportImprovements || NormalConfig.instance.rewriteLoggingWithDeobfuscatedNames) {
            File modDir = new File(Launch.minecraftHome, "config/normalasm");
            modDir.mkdirs();
            // Initialize StacktraceDeobfuscator
            NormalLogger.instance.info("Initializing StacktraceDeobfuscator...");
            try {
                File mappings = new File(modDir, "methods-stable_39.csv");
                if (mappings.exists()) {
                    NormalLogger.instance.info("Found MCP stable-39 method mappings: {}", mappings.getName());
                } else {
                    NormalLogger.instance.info("Downloading MCP stable-39 method mappings to: {}", mappings.getName());
                }
                StacktraceDeobfuscator.init(mappings);
            } catch (Exception e) {
                NormalLogger.instance.error("Failed to get MCP stable-39 data!", e);
            }
            NormalLogger.instance.info("Initialized StacktraceDeobfuscator.");
            if (NormalConfig.instance.rewriteLoggingWithDeobfuscatedNames) {
                NormalLogger.instance.info("Installing DeobfuscatingRewritePolicy...");
                DeobfuscatingRewritePolicy.install();
                NormalLogger.instance.info("Installed DeobfuscatingRewritePolicy.");
            }
        }
        boolean needToDGSFFFF = isVMOpenJ9 && SystemUtils.IS_JAVA_1_8;
        int buildAppendIndex = SystemUtils.JAVA_VERSION.indexOf("_");
        if (needToDGSFFFF && buildAppendIndex != -1) {
            if (Integer.parseInt(SystemUtils.JAVA_VERSION.substring(buildAppendIndex + 1)) < 265) {
                for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                    if (arg.equals("-Xjit:disableGuardedStaticFinalFieldFolding")) {
                        needToDGSFFFF = false;
                        break;
                    }
                }
                if (needToDGSFFFF) {
                    NormalLogger.instance.fatal("FermiumASM notices that you're using Eclipse OpenJ9 {}!", SystemUtils.JAVA_VERSION);
                    NormalLogger.instance.fatal("This OpenJ9 version is outdated and contains a critical bug: https://github.com/eclipse-openj9/openj9/issues/8353");
                    NormalLogger.instance.fatal("Either use '-Xjit:disableGuardedStaticFinalFieldFolding' as part of your java arguments, or update OpenJ9!");
                }
            }
        }

        NormalLogger.instance.info("FermiumASM enqueueing early mixins");
        for(String config : earlyList) {
            FermiumRegistryAPI.enqueueMixin(false, config, () -> shouldMixinConfigQueueEarly(config));
        }
        NormalLogger.instance.info("FermiumASM enqueueing late mixins");
        for(String config : lateList) {
            FermiumRegistryAPI.enqueueMixin(true, config, () -> shouldMixinConfigQueueLate(config));
        }
        NormalLogger.instance.info("FermiumASM finished mixin enqueue");
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return "mirror.normalasm.core.NormalFMLCallHook";
    }

    @Override
    public void injectData(Map<String, Object> data) { }

    @Override
    public String getAccessTransformerClass() {
        return "mirror.normalasm.core.NormalTransformer";
    }

    private static final List<String> earlyList = Arrays.asList(
            //Devenv
            "mixins.devenv.json",

            //Both
            "mixins.internal.json",//Default true
            "mixins.vanities.json",//Default true
            "mixins.registries.json",
            "mixins.stripitemstack.json",
            "mixins.lockcode.json",
            "mixins.recipes.json",
            "mixins.misc_fluidregistry.json",
            "mixins.forgefixes.json",
            "mixins.capability.json",
            "mixins.singletonevents.json",
            "mixins.efficienthashing.json",
            "mixins.crashes.json",
            "mixins.fix_mc129057.json",
            "mixins.priorities.json",

            //Client
            "mixins.bucket.json",
            "mixins.rendering.json",
            "mixins.datastructures_modelmanager.json",
            "mixins.screenshot.json",
            "mixins.ondemand_sprites.json",
            "mixins.searchtree_vanilla.json",//Default true
            "mixins.resolve_mc2071.json",
            "mixins.fix_mc_skindownloading.json",
            "mixins.fix_mc186052.json",//Default true

            //Non-client
            "mixins.vfix_bugfixes.json"//Default true
    );

    public static boolean shouldMixinConfigQueueEarly(String mixinConfig) {
        if ("mixins.devenv.json".equals(mixinConfig)) {
            return FMLLaunchHandler.isDeobfuscatedEnvironment();//Previously always applied devenv.json due to checking env first, oversight?
        }

        if (isClient) {
            switch (mixinConfig) {
                case "mixins.bucket.json":
                    return NormalConfig.instance.reuseBucketQuads;
                case "mixins.rendering.json":
                    return NormalConfig.instance.optimizeSomeRendering;
                case "mixins.datastructures_modelmanager.json":
                    return NormalConfig.instance.moreModelManagerCleanup;
                case "mixins.screenshot.json":
                    return NormalConfig.instance.releaseScreenshotCache || NormalConfig.instance.asyncScreenshot;
                case "mixins.resolve_mc2071.json":
                    return NormalConfig.instance.resolveMC2071;
                case "mixins.fix_mc_skindownloading.json":
                    return NormalConfig.instance.limitSkinDownloadingThreads;
                case "mixins.searchtree_vanilla.json":
                    return true;
                case "mixins.fix_mc186052.json":
                    return true;
                case "mixins.fix_mc129556.json":
                    return NormalConfig.instance.fixMC129556;
            }
        }

        switch (mixinConfig) {
            case "mixins.registries.json":
                return NormalConfig.instance.optimizeRegistries;
            case "mixins.stripitemstack.json":
                return NormalConfig.instance.stripNearUselessItemStackFields;
            case "mixins.lockcode.json":
                return NormalConfig.instance.lockCodeCanonicalization;
            case "mixins.recipes.json":
                return NormalConfig.instance.optimizeFurnaceRecipeStore;
            case "mixins.misc_fluidregistry.json":
                return NormalConfig.instance.quickerEnableUniversalBucketCheck;
            case "mixins.forgefixes.json":
                return NormalConfig.instance.fixFillBucketEventNullPointerException ||
                        NormalConfig.instance.fixTileEntityOnLoadCME ||
                        NormalConfig.instance.fasterEntitySpawnPreparation ||
                        NormalConfig.instance.fixDimensionTypesInliningCrash;
            case "mixins.capability.json":
                return NormalConfig.instance.delayItemStackCapabilityInit;
            case "mixins.singletonevents.json":
                return NormalConfig.instance.makeEventsSingletons;
            case "mixins.efficienthashing.json":
                return NormalConfig.instance.efficientHashing;
            case "mixins.crashes.json":
                return NormalConfig.instance.crashReportImprovements;
            case "mixins.fix_mc129057.json":
                return NormalConfig.instance.fixMC129057;
            case "mixins.priorities.json":
                return NormalConfig.instance.threadPriorityFix;
            case "mixins.internal.json":
                return true;
            case "mixins.vanities.json":
                return true;
            case "mixins.vfix_bugfixes.json":
                return NormalConfig.instance.vfixBugFixes;
        }

        return false;
    }

    private static final List<String> lateList = Arrays.asList(
            "mixins.bakedquadsquasher.json",
            "mixins.modfixes_immersiveengineering.json",
            "mixins.modfixes_evilcraftcompat.json",
            "mixins.modfixes_ebwizardry.json",
            "mixins.modfixes_xu2.json",
            "mixins.searchtree_mod.json",
            "mixins.modfixes_astralsorcery.json",
            "mixins.capability_astralsorcery.json",
            "mixins.modfixes_b3m.json"
    );

    public static boolean shouldMixinConfigQueueLate(String mixinConfig) {
        switch (mixinConfig) {
            case "mixins.bakedquadsquasher.json":
                return NormalTransformer.squashBakedQuads;
            case "mixins.modfixes_immersiveengineering.json":
                return NormalConfig.instance.fixBlockIEBaseArrayIndexOutOfBoundsException && Loader.isModLoaded("immersiveengineering");
            case "mixins.modfixes_evilcraftcompat.json":
                return NormalConfig.instance.repairEvilCraftEIOCompat && Loader.isModLoaded("evilcraftcompat") && Loader.isModLoaded("enderio") &&
                        Loader.instance().getIndexedModList().get("enderio").getVersion().equals("5.3.70"); // Only apply on newer EIO versions where compat was broken
            case "mixins.modfixes_ebwizardry.json":
                return NormalConfig.instance.optimizeArcaneLockRendering && Loader.isModLoaded("ebwizardry");
            case "mixins.modfixes_xu2.json":
                return (NormalConfig.instance.fixXU2CrafterCrash || NormalConfig.instance.disableXU2CrafterRendering) && Loader.isModLoaded("extrautils2");
            case "mixins.searchtree_mod.json":
                return NormalConfig.instance.replaceSearchTreeWithJEISearching && Loader.isModLoaded("jei");
            case "mixins.modfixes_astralsorcery.json":
                return NormalConfig.instance.optimizeAmuletRelatedFunctions && Loader.isModLoaded("astralsorcery");
            case "mixins.capability_astralsorcery.json":
                return NormalConfig.instance.fixAmuletHolderCapability && Loader.isModLoaded("astralsorcery");
            case "mixins.modfixes_b3m.json":
                return NormalConfig.instance.resourceLocationCanonicalization && Loader.isModLoaded("B3M"); // Stupid
        }

        return false;
    }
}