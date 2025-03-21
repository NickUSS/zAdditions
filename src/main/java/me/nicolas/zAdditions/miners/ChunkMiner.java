package me.nicolas.zAdditions.miners;

import me.nicolas.zAdditions.ZAdditions;
import me.nicolas.zAdditions.items.ChunkMinerItem;
import me.nicolas.zAdditions.utils.WorldGuardUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;

public class ChunkMiner {
    private final Location location;
    private final Chunk chunk;
    private UUID ownerUUID;
    private final TextDisplay titleHologram;
    private final TextDisplay progressHologram;
    private final TextDisplay statsHologram;
    private BukkitTask miningTask;
    private BukkitTask optimizationTask;
    private int currentX;
    private int currentY;
    private int currentZ;
    private int blocksMinedCount;
    private boolean isActive;
    private final List<Inventory> storagePages;
    private Block currentBlock;
    private int miningProgress;
    private int animationFrame = 0;
    private int tickCounter = 0;
    private int currentPage = 0;
    private boolean isPaused = false;

    private static final int STORAGE_SIZE = 54;
    private static final int MINING_TICKS = 10; // Medio segundo (10 ticks)
    private static final String[] ANIMATION_FRAMES = {
            "§e⚒ §6Minando §e⚒",
            "§e⚒ §6Minando. §e⚒",
            "§e⚒ §6Minando.. §e⚒",
            "§e⚒ §6Minando... §e⚒"
    };
    private static final String PROGRESS_BAR_CHAR = "█";
    private static final int PROGRESS_BAR_LENGTH = 20;

    public ChunkMiner(Location location, UUID ownerUUID) {
        this.location = location;
        this.chunk = location.getChunk();
        this.ownerUUID = ownerUUID;
        this.currentX = chunk.getX() * 16;
        this.currentY = chunk.getWorld().getMaxHeight() - 1;
        this.currentZ = chunk.getZ() * 16;
        this.blocksMinedCount = 0;
        this.isActive = true;
        this.storagePages = new ArrayList<>();
        this.miningProgress = 0;

        // Crear primera página de almacenamiento
        createNewStoragePage();

        // Limpiar hologramas existentes
        location.getWorld().getNearbyEntities(location, 3, 3, 3).forEach(entity -> {
            if (entity instanceof TextDisplay) {
                entity.remove();
            }
        });

        // Crear hologramas usando TextDisplay con diferentes alturas
        this.titleHologram = location.getWorld().spawn(
                location.clone().add(0.5, 2.5, 0.5),  // Más alto
                TextDisplay.class,
                display -> {
                    display.setBillboard(Display.Billboard.CENTER);
                    display.setAlignment(TextDisplay.TextAlignment.CENTER);
                    display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                    display.setSeeThrough(true);
                    display.setDefaultBackground(false);
                    display.setText("§6§lChunk Miner");
                }
        );

        this.progressHologram = location.getWorld().spawn(
                location.clone().add(0.5, 2.2, 0.5),  // En medio
                TextDisplay.class,
                display -> {
                    display.setBillboard(Display.Billboard.CENTER);
                    display.setAlignment(TextDisplay.TextAlignment.CENTER);
                    display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                    display.setSeeThrough(true);
                    display.setDefaultBackground(false);
                }
        );

        this.statsHologram = location.getWorld().spawn(
                location.clone().add(0.5, 1.9, 0.5),  // Más bajo
                TextDisplay.class,
                display -> {
                    display.setBillboard(Display.Billboard.CENTER);
                    display.setAlignment(TextDisplay.TextAlignment.CENTER);
                    display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                    display.setSeeThrough(true);
                    display.setDefaultBackground(false);
                }
        );

        setupHologram();
        startMining();
        scheduleStorageOptimization();
    }

    public void pause() {
        isPaused = true;
        if (miningTask != null) {
            miningTask.cancel();
            miningTask = null;
        }
    }

    public void resume() {
        if (!isActive) return;

        isPaused = false;
        if (miningTask == null) {
            startMining();
        }
    }

    private void createNewStoragePage() {
        int pageNumber = storagePages.size() + 1;
        Inventory newPage = Bukkit.createInventory(null, STORAGE_SIZE,
                String.format("§6Chunk Miner Storage §7(Página %d)", pageNumber));

        // Reservar la última fila para navegación
        ItemStack barrier = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta barrierMeta = barrier.getItemMeta();
        barrierMeta.setDisplayName("§8•");
        barrier.setItemMeta(barrierMeta);

        for (int i = 45; i < 54; i++) {
            newPage.setItem(i, barrier);
        }

        // Agregar botones de navegación
        if (!storagePages.isEmpty()) {
            // Botón página anterior
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevButton.getItemMeta();
            prevMeta.setDisplayName("§ePágina Anterior");
            prevButton.setItemMeta(prevMeta);
            newPage.setItem(45, prevButton);

            // Actualizar última página con botón siguiente
            ItemStack nextButton = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextButton.getItemMeta();
            nextMeta.setDisplayName("§ePágina Siguiente");
            nextButton.setItemMeta(nextMeta);
            storagePages.get(storagePages.size() - 1).setItem(53, nextButton);
        }

        // Indicador de página actual
        ItemStack pageIndicator = new ItemStack(Material.PAPER);
        ItemMeta pageMeta = pageIndicator.getItemMeta();
        pageMeta.setDisplayName(String.format("§ePágina %d", pageNumber));
        pageIndicator.setItemMeta(pageMeta);
        newPage.setItem(49, pageIndicator);

        storagePages.add(newPage);
    }

    private void scheduleStorageOptimization() {
        optimizationTask = Bukkit.getScheduler().runTaskTimer(ZAdditions.getInstance(),
                this::optimizeStorage, 6000L, 6000L); // Cada 5 minutos
    }

    private void optimizeStorage() {
        List<ItemStack> allItems = new ArrayList<>();

        // Recolectar todos los items
        for (Inventory page : storagePages) {
            for (int i = 0; i < page.getSize() - 9; i++) {
                ItemStack item = page.getItem(i);
                if (item != null) {
                    allItems.add(item.clone());
                    page.clear(i);
                }
            }
        }

        // Limpiar todas las páginas excepto la primera
        while (storagePages.size() > 1) {
            storagePages.remove(storagePages.size() - 1);
        }

        // Redistribuir los items
        for (ItemStack item : allItems) {
            addItemToStorage(item);
        }
    }

    private boolean hasSpaceInStorage(ItemStack item) {
        // Buscar espacio desde la primera página
        for (int i = 0; i < storagePages.size(); i++) {
            Inventory page = storagePages.get(i);
            if (page.firstEmpty() != -1 || page.containsAtLeast(item, item.getAmount())) {
                currentPage = i;
                return true;
            }
        }

        // Si no hay espacio en ninguna página existente, crear una nueva
        createNewStoragePage();
        currentPage = storagePages.size() - 1;
        return true;
    }

    private void addItemToStorage(ItemStack item) {
        // Intentar agregar el item empezando desde la primera página
        ItemStack remaining = item.clone();

        for (int i = 0; i < storagePages.size() && remaining.getAmount() > 0; i++) {
            Inventory page = storagePages.get(i);

            // Buscar stacks similares que no estén llenos
            for (int slot = 0; slot < page.getSize() - 9 && remaining.getAmount() > 0; slot++) {
                ItemStack slotItem = page.getItem(slot);
                if (slotItem != null && slotItem.isSimilar(remaining) &&
                        slotItem.getAmount() < slotItem.getMaxStackSize()) {
                    int canAdd = slotItem.getMaxStackSize() - slotItem.getAmount();
                    int toAdd = Math.min(canAdd, remaining.getAmount());

                    slotItem.setAmount(slotItem.getAmount() + toAdd);
                    remaining.setAmount(remaining.getAmount() - toAdd);
                }
            }

            // Si aún quedan items y hay slots vacíos, usar slots vacíos
            if (remaining.getAmount() > 0) {
                int firstEmpty = page.firstEmpty();
                while (firstEmpty != -1 && remaining.getAmount() > 0 && firstEmpty < page.getSize() - 9) {
                    int toAdd = Math.min(remaining.getAmount(), remaining.getMaxStackSize());
                    ItemStack toPlace = remaining.clone();
                    toPlace.setAmount(toAdd);
                    page.setItem(firstEmpty, toPlace);
                    remaining.setAmount(remaining.getAmount() - toAdd);
                    firstEmpty = page.firstEmpty();
                }
            }
        }

        // Si aún quedan items, crear una nueva página
        if (remaining.getAmount() > 0) {
            createNewStoragePage();
            currentPage = storagePages.size() - 1;
            addItemToStorage(remaining);
        }
    }

    private void setupHologram() {
        updateHologram();
    }

    private void updateHologram() {
        if (currentBlock != null) {
            // Título animado
            titleHologram.setText(ANIMATION_FRAMES[animationFrame]);
            animationFrame = (animationFrame + 1) % ANIMATION_FRAMES.length;

            // Progreso del bloque actual
            String blockName = formatBlockName(currentBlock.getType().name());
            progressHologram.setText(String.format("§7Minando: §e%s", blockName));

            // Estadísticas
            statsHologram.setText(String.format("§eY: §f%d §8| §e%d §7bloques minados",
                    currentY, blocksMinedCount));
        } else {
            // Cuando no está minando
            titleHologram.setText("§6§lChunk Miner");

            // Progreso total
            int totalBlocks = (chunk.getWorld().getMaxHeight() - chunk.getWorld().getMinHeight()) * 256;
            double totalProgress = (double) blocksMinedCount / totalBlocks * 100;
            progressHologram.setText(String.format("§7Progreso: §e%.1f%%", totalProgress));

            // Estadísticas
            statsHologram.setText(String.format("§e%d §7bloques minados §8| §eY: §f%d",
                    blocksMinedCount, currentY));
        }
    }

    private String formatBlockName(String name) {
        return Arrays.stream(name.toLowerCase().split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    private String createProgressBar(double progress) {
        int filledBars = (int) (PROGRESS_BAR_LENGTH * progress);
        StringBuilder bar = new StringBuilder();

        bar.append("§a");
        for (int i = 0; i < filledBars; i++) {
            bar.append(PROGRESS_BAR_CHAR);
        }

        bar.append("§7");
        for (int i = filledBars; i < PROGRESS_BAR_LENGTH; i++) {
            bar.append(PROGRESS_BAR_CHAR);
        }

        return bar.toString();
    }

    private void startMining() {
        if (miningTask != null) {
            miningTask.cancel();
            miningTask = null;
        }

        miningTask = Bukkit.getScheduler().runTaskTimer(ZAdditions.getInstance(), () -> {
            if (!isActive || isPaused || !isOwnerOnline()) {
                pause();
                return;
            }

            if (currentBlock == null) {
                findNextBlock();
            } else {
                continueMiningBlock();
            }

            tickCounter++;
            if (tickCounter % 5 == 0) {
                updateHologram();
            }
        }, 0L, 1L);
    }

    public boolean isPaused() {
        return isPaused;
    }

    private void findNextBlock() {
        while (currentY > chunk.getWorld().getMinHeight()) {
            Block block = chunk.getWorld().getBlockAt(currentX, currentY, currentZ);

            if (shouldMineBlock(block)) {
                currentBlock = block;
                miningProgress = 0;
                return;
            }

            moveToNextPosition();
        }

        finishMining();
    }

    private void finishMining() {
        isActive = false;
        if (miningTask != null) {
            miningTask.cancel();
            miningTask = null;
        }

        Location loc = location.clone().add(0.5, 0.5, 0.5);
        loc.getWorld().spawnParticle(
                Particle.EXPLOSION,
                loc,
                3,
                0.3, 0.3, 0.3,
                0
        );

        loc.getWorld().spawnParticle(
                Particle.FLAME,
                loc,
                20,
                0.5, 0.5, 0.5,
                0.1
        );

        loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);

        // Actualizar hologramas con mensaje final
        titleHologram.setText("§6§l¡Completado!");
        progressHologram.setText("§e" + blocksMinedCount + " §7bloques minados");
        statsHologram.setText("§7¡Gracias por usar Chunk Miner!");

        // Programar la eliminación después de mostrar el mensaje final
        Bukkit.getScheduler().runTaskLater(ZAdditions.getInstance(), () -> {
            Player owner = Bukkit.getPlayer(ownerUUID);
            if (owner != null) {
                owner.getInventory().addItem(ChunkMinerItem.create());
                owner.sendMessage("§a¡Chunk Miner ha terminado de minar el chunk!");
            }

            cleanup();
        }, 60L); // 3 segundos de delay
    }

    private void moveToNextPosition() {
        currentX++;
        if (currentX >= chunk.getX() * 16 + 16) {
            currentX = chunk.getX() * 16;
            currentZ++;
            if (currentZ >= chunk.getZ() * 16 + 16) {
                currentZ = chunk.getZ() * 16;
                currentY--;
            }
        }
    }

    private void continueMiningBlock() {
        if (currentBlock == null) return;

        miningProgress++;

        Location blockLoc = currentBlock.getLocation().add(0.5, 0.5, 0.5);
        Location beaconLoc = location.clone().add(0.5, 0.5, 0.5);

        // Dibujar láser
        drawLaser(beaconLoc, blockLoc);

        // Partículas de minado (diferentes para agua)
        if (miningProgress % 2 == 0) {
            if (currentBlock.isLiquid()) {
                currentBlock.getWorld().spawnParticle(
                        Particle.SPLASH,
                        blockLoc,
                        5,
                        0.2, 0.2, 0.2,
                        0
                );
            } else {
                currentBlock.getWorld().spawnParticle(
                        Particle.SMOKE,
                        blockLoc,
                        3,
                        0.2, 0.2, 0.2,
                        0
                );
            }
        }

        // Sonido de minado
        if (miningProgress % 5 == 0) {
            currentBlock.getWorld().playSound(
                    blockLoc,
                    currentBlock.isLiquid() ? Sound.ITEM_BUCKET_EMPTY : Sound.BLOCK_STONE_HIT,
                    0.2f,
                    1.0f
            );
        }

        if (miningProgress >= MINING_TICKS) {
            mineCurrentBlock();
        }
    }

    private void drawLaser(Location start, Location end) {
        Vector direction = end.toVector().subtract(start.toVector());
        double distance = direction.length();
        direction.normalize();

        for (double i = 0; i < distance; i += 0.5) {
            Vector point = start.toVector().add(direction.clone().multiply(i));
            start.getWorld().spawnParticle(
                    Particle.FLAME,
                    point.getX(), point.getY(), point.getZ(),
                    1,
                    0, 0, 0,
                    0
            );
        }
    }

    private void mineCurrentBlock() {
        if (currentBlock == null) return;

        // Limpiar agua alrededor antes de minar
        clearWaterAround(currentBlock.getLocation());

        // Recolectar drops (solo si no es agua)
        if (!currentBlock.isLiquid()) {
            Collection<ItemStack> drops = currentBlock.getDrops();
            for (ItemStack drop : drops) {
                if (hasSpaceInStorage(drop)) {
                    addItemToStorage(drop);
                } else {
                    currentBlock.getWorld().dropItemNaturally(currentBlock.getLocation(), drop);
                }
            }
        }

        Location blockLoc = currentBlock.getLocation().add(0.5, 0.5, 0.5);

        // Efectos de ruptura
        currentBlock.getWorld().playSound(
                blockLoc,
                Sound.BLOCK_STONE_BREAK,
                0.5f,
                1.0f
        );

        // Efectos de partículas solo si no es agua
        if (!currentBlock.isLiquid()) {
            currentBlock.getWorld().spawnParticle(
                    Particle.LAVA,
                    blockLoc,
                    8,
                    0.2, 0.2, 0.2,
                    0
            );

            currentBlock.getWorld().spawnParticle(
                    Particle.SMOKE,
                    blockLoc,
                    12,
                    0.2, 0.2, 0.2,
                    0
            );
        }

        currentBlock.setType(Material.AIR);
        blocksMinedCount++;
        currentBlock = null;
        miningProgress = 0;
        moveToNextPosition();
    }

    private void clearWaterAround(Location center) {
        int radius = 5; // Radio aumentado a 5 bloques
        int verticalRadius = 3; // Radio vertical para cubrir más altura
        World world = center.getWorld();
        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        // Primera pasada: Limpiar fuentes de agua
        for (int x = -radius; x <= radius; x++) {
            for (int y = -verticalRadius; y <= verticalRadius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location loc = new Location(world, centerX + x, centerY + y, centerZ + z);
                    Block block = loc.getBlock();

                    if (block.isLiquid()) {
                        // Si es una fuente de agua o está en el chunk
                        if (block.getChunk().equals(chunk) ||
                                isWaterSource(block) ||
                                block.getType() == Material.LAVA) {

                            clearLiquidBlock(block);
                        }
                    }
                }
            }
        }

        // Segunda pasada: Limpiar agua fluyendo
        for (int pass = 0; pass < 2; pass++) { // Hacer dos pasadas para asegurar
            for (int x = -radius; x <= radius; x++) {
                for (int y = -verticalRadius; y <= verticalRadius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        Location loc = new Location(world, centerX + x, centerY + y, centerZ + z);
                        Block block = loc.getBlock();

                        if (block.isLiquid()) {
                            clearLiquidBlock(block);
                        }
                    }
                }
            }
        }
    }

    private void clearLiquidBlock(Block block) {
        Location loc = block.getLocation().add(0.5, 0.5, 0.5);
        World world = block.getWorld();

        // Diferentes efectos según el tipo de líquido
        if (block.getType() == Material.WATER) {
            // Efecto de drenado de agua
            world.spawnParticle(
                    Particle.SPLASH,
                    loc,
                    15, // Más partículas
                    0.3, 0.3, 0.3,
                    0
            );
            world.spawnParticle(
                    Particle.BUBBLE,
                    loc,
                    10,
                    0.2, 0, 0.2,
                    0
            );

            // Sonido de drenado de agua
            world.playSound(
                    loc,
                    Sound.ITEM_BUCKET_EMPTY,
                    0.2f,
                    1.0f
            );
        } else if (block.getType() == Material.LAVA) {
            // Efecto de enfriamiento de lava
            world.spawnParticle(
                    Particle.SMOKE,
                    loc,
                    10,
                    0.2, 0.2, 0.2,
                    0
            );
            world.spawnParticle(
                    Particle.LAVA,
                    loc,
                    5,
                    0.2, 0.2, 0.2,
                    0
            );

            // Sonido de lava enfriándose
            world.playSound(
                    loc,
                    Sound.BLOCK_LAVA_EXTINGUISH,
                    0.2f,
                    1.0f
            );
        }

        // Remover el líquido
        block.setType(Material.AIR);
    }

    private boolean isWaterSource(Block block) {
        if (block.getType() != Material.WATER) return false;

        if (block.getBlockData() instanceof Levelled) {
            Levelled levelledBlock = (Levelled) block.getBlockData();
            return levelledBlock.getLevel() == 0; // Nivel 0 indica fuente de agua
        }
        return false;
    }

    private boolean shouldMineBlock(Block block) {
        // Verificar restricciones básicas
        if (block.getType().isAir() ||
                block.getType().equals(Material.BEDROCK) ||
                block.getType().equals(Material.BEACON) ||
                block.getType().name().contains("SPAWNER")) {
            return false;
        }

        // Verificar restricciones de WorldGuard si está habilitado
        ZAdditions plugin = ZAdditions.getInstance();
        if (plugin.isWorldGuardEnabled()) {
            // Si está en región global (sin protección), permitir minar
            if (WorldGuardUtils.isInGlobalRegion(block)) {
                return true;
            }

            // Si está en una región protegida, solo permitir si el dueño es el mismo
            Player owner = Bukkit.getPlayer(ownerUUID);
            if (owner == null) {
                // Si el dueño no está online, no minar en regiones protegidas
                return false;
            }

            // Solo permitir minar si el dueño del miner es dueño de la región
            return WorldGuardUtils.isOwner(owner, block.getLocation());
        }

        // Si WorldGuard no está habilitado, permitir minar cualquier bloque válido
        return true;
    }

    private boolean isOwnerOnline() {
        return Bukkit.getPlayer(ownerUUID) != null;
    }

    public void openStorage(Player player) {
        player.openInventory(storagePages.get(0));
    }

    public void remove(boolean dropItem) {
        cleanup();
    }

    public void cleanup() {
        isActive = false;
        if (miningTask != null) {
            miningTask.cancel();
            miningTask = null;
        }
        if (optimizationTask != null) {
            optimizationTask.cancel();
            optimizationTask = null;
        }

        if (location.getBlock().getType() == Material.BEACON) {
            location.getBlock().setType(Material.AIR);
        }

        // Remover hologramas de forma segura
        if (titleHologram != null && !titleHologram.isDead()) {
            titleHologram.remove();
        }
        if (progressHologram != null && !progressHologram.isDead()) {
            progressHologram.remove();
        }
        if (statsHologram != null && !statsHologram.isDead()) {
            statsHologram.remove();
        }

        // Limpiar entidades cercanas por si acaso
        location.getWorld().getNearbyEntities(location, 3, 3, 3).forEach(entity -> {
            if (entity instanceof TextDisplay) {
                entity.remove();
            }
        });
    }

    public Inventory getStoragePage(int page) {
        if (page >= 0 && page < storagePages.size()) {
            return storagePages.get(page);
        }
        return null;
    }

    public List<Inventory> getStoragePages() {
        return storagePages;
    }

    public Location getLocation() {
        return location;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void setOwner(UUID newOwner) {
        this.ownerUUID = newOwner;
    }

    public double getProgress() {
        int totalBlocks = (chunk.getWorld().getMaxHeight() - chunk.getWorld().getMinHeight()) * 256;
        return (double) blocksMinedCount / totalBlocks * 100;
    }

    public boolean isActive() {
        return isActive;
    }
}