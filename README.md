# FermiumASM
FermiumASM is now preparing to bytecode manipulate your game.

FermiumASM is a fork of [NormalASM](https://github.com/skmedix/NormalASM) by skmedix;
Which is an updated fork of [NormalASM](https://github.com/mirrorcult/NormalASM) by mirrorcult;
Which is a rebranding to remove pedophilic content fork of [LoliASM](https://github.com/LoliKingdom/LoliASM) by Rongmario

While NormalASM only rebrands and maintains updated with LoliASM, FermiumASM does make more significant changes.

Significant changes:
 - Refactored to use and depend on FermiumBooter instead of MixinBooter, an MIT alternative to late mixin loading
 - Modified to remove dependency on MixinExtras
 - Removed built-in Spark profiling as it is both incompatible with, and superseded by, the updated fork Spark Unforged
 - Rebranded user-facing and logging branding to FermiumASM for distribution
 - Removed chance to pick and blame a random author in a crash
 - Updated mcmod.info to add fork authors, update url, and change icon
 - (Internal) Reverted to Gradle 7.5
 - (Internal) Transitioned to FG5.1+
 - (Internal) Transitioned to Mixin 0.8.5