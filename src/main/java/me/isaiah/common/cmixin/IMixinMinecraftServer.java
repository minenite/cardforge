package me.isaiah.common.cmixin;

import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.commands.Commands.CommandSelection;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import com.mojang.authlib.GameProfile;

public interface IMixinMinecraftServer {

    /**
     * note: unused?
     */
    // public RegistryOps<Object> Iof(DynamicOps<Object> delegate, ResourceManager resourceManager, Impl impl);

    /**
     * 1.17/1.18 safe replacement for GeneratorOptions.createOverworldGenerator
     * 
     * @implNote 1.18 - createOverworldGenerator(DynamicRegistryManager, long)
     * @implNote 1.17 - createOverworldGenerator(Registry<Biome>, Registry<ChunkGeneratorSettings>, long)
     */
    public NoiseBasedChunkGenerator I_createOverworldGenerator();

    /**
     * The constructors for ChunkSection have changed
     * between 1.17 and 1.18. This method provides the
     * 1.17 arguments for 1.18
     * 
     * @implNote 1.17 - ChunkSection(int yOffset)
     * @implNote 1.18 - ChunkSection(int chunkPos, Registry<Biome> biomeRegistry)
     */
    public LevelChunkSection newChunkSection(int yOffset);

    /**
     * Retrieve UUID from a GameProfile
     * 
     * @implNote 1.18 -
     * @implNote 1.19 -
     */
    public UUID get_uuid_from_profile(GameProfile profile);

    /**
     * Create new instance of CommandManager
     * 
     * @implNote 1.18 - new CommandManager(RegistrationEnvironment)
     * @implNote 1.19 - new CommandManager(RegistrationEnvironment, CommandRegistryAccess)
     */
    public Commands new_command_manager(CommandSelection env);
    
    /**
     */
    public MerchantOffer create_new_trade_offer(ItemStack result, int uses, int maxUses, boolean experienceReward, int experience, float priceMultiplier, int demand, int specialPrice);

    /**
     */
    public ClientboundUpdateMobEffectPacket new_status_effect_packet(int id, MobEffectInstance effect, boolean bl);

    /**
     */
    public Component IC$from_json(String json); // Serialization
    
    /**
     */
    public String IC$to_json(Component text);
    
    /**
     */
    public int IC$get_connection_state(ClientIntentionPacket packet);
    
    /**
     */
    // public Identifier IC$get_loot_table_id(LootableContainerBlockEntity bl);

    /**
     */
    public BlockEntity IC$create_blockentity_from_nbt(BlockPos pos, BlockState state, CompoundTag nbt);
}