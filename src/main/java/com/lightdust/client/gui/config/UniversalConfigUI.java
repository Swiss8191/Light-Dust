package com.lightdust.client.gui.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class UniversalConfigUI {

    public static class Builder {
        private final Component title;
        private final Screen parent;
        private final Map<String, List<ConfigEntry>> categories = new LinkedHashMap<>();
        private final Map<String, List<SectionTarget>> sectionTargets = new HashMap<>();
        private final List<ForgeConfigSpec> specs = new ArrayList<>();
        
        private BooleanEntry activeDepEntry;
        private String activeDepName;

        public Builder(Component title, Screen parent) {
            this.title = title;
            this.parent = parent;
        }

        public Builder withSpec(ForgeConfigSpec spec) {
            if (spec == null) throw new IllegalArgumentException("withSpec: spec must not be null");
            specs.add(spec);
            return this;
        }

        public Builder pushDependency(ForgeConfigSpec.ConfigValue<Boolean> parentConfig, String parentLabel) {
            for (List<ConfigEntry> catList : categories.values()) {
                for (int i = catList.size() - 1; i >= 0; i--) {
                    ConfigEntry entry = catList.get(i);
                    if (entry instanceof BooleanEntry be && be.label.equals(parentLabel)) {
                        this.activeDepEntry = be;
                        this.activeDepName = parentLabel;
                        return this;
                    }
                }
            }
            return this;
        }

        public Builder popDependency() {
            this.activeDepEntry = null;
            this.activeDepName = null;
            return this;
        }

        private List<ConfigEntry> getCat(String cat) {
            if (cat == null || cat.isBlank())
                throw new IllegalArgumentException("category name must not be null or blank");
            categories.putIfAbsent(cat, new ArrayList<>());
            return categories.get(cat);
        }

        private void requireArgs(String context, String category, String label, Object config) {
            List<String> missing = new ArrayList<>();
            if (category == null || category.isBlank()) missing.add("category");
            if (label    == null || label.isBlank())    missing.add("label");
            if (config   == null)                       missing.add("config");
            if (!missing.isEmpty())
                throw new IllegalArgumentException(
                    context + ": missing required argument(s): " + String.join(", ", missing)
                    + " [category=" + category + ", label=" + label + "]");
        }

        public Builder beginSection(String category, String sectionTitle, String description) {
            if (category     == null || category.isBlank())
                throw new IllegalArgumentException("beginSection: category must not be null or blank");
            if (sectionTitle == null || sectionTitle.isBlank())
                throw new IllegalArgumentException("beginSection: sectionTitle must not be null or blank (category=" + category + ")");
            List<ConfigEntry> catList = getCat(category);
            if (!catList.isEmpty()) catList.add(new SpacerEntry());
            SectionHeaderEntry header = new SectionHeaderEntry(sectionTitle);
            catList.add(header);
            if (description != null && !description.isEmpty()) catList.add(new SectionDescEntry(description));
            else catList.add(new SectionDescEntry(""));
            sectionTargets.putIfAbsent(category, new ArrayList<>());
            sectionTargets.get(category).add(new SectionTarget(sectionTitle, header));
            return this;
        }

        public Builder addBoolean(String category, String label, String comment, int restartReq, ForgeConfigSpec.ConfigValue<Boolean> config) {
            requireArgs("addBoolean", category, label, config);
            BooleanEntry entry = new BooleanEntry(label, comment, restartReq, config);
            if (activeDepEntry != null) entry.dependsOn(activeDepEntry, activeDepName);
            getCat(category).add(entry); return this;
        }

        public Builder addNumber(String category, String label, String comment, int restartReq, ForgeConfigSpec.ConfigValue<? extends Number> config) {
            requireArgs("addNumber", category, label, config);
            NumberEntry entry = new NumberEntry(label, comment, restartReq, config);
            if (activeDepEntry != null) entry.dependsOn(activeDepEntry, activeDepName);
            getCat(category).add(entry); return this;
        }

        public Builder addString(String category, String label, String comment, int restartReq, ForgeConfigSpec.ConfigValue<String> config) {
            requireArgs("addString", category, label, config);
            StringConfigEntry entry = new StringConfigEntry(label, comment, restartReq, config);
            if (activeDepEntry != null) entry.dependsOn(activeDepEntry, activeDepName);
            getCat(category).add(entry); return this;
        }

        public <T extends Enum<T>> Builder addEnum(String category, String label, String comment, int restartReq, ForgeConfigSpec.EnumValue<T> config, Class<T> enumClass) {
            requireArgs("addEnum", category, label, config);
            if (enumClass == null) throw new IllegalArgumentException("addEnum: enumClass must not be null (label=" + label + ")");
            EnumEntry<T> entry = new EnumEntry<>(label, comment, restartReq, config, enumClass);
            if (activeDepEntry != null) entry.dependsOn(activeDepEntry, activeDepName);
            getCat(category).add(entry); return this;
        }

        public Builder addStringList(String category, String label, String comment, int restartReq, ForgeConfigSpec.ConfigValue<List<? extends String>> config) {
            requireArgs("addStringList", category, label, config);
            ListEntry entry = new ListEntry(label, comment, restartReq, config);
            if (activeDepEntry != null) entry.dependsOn(activeDepEntry, activeDepName);
            getCat(category).add(entry); return this;
        }

        public Screen build() {
            if (categories.isEmpty())
                throw new IllegalStateException("build(): no categories have been added  --  call addBoolean/addNumber/etc. first");
            return new ConfigScreen(title, parent, categories, sectionTargets, specs);
        }
    }

    public static class SectionTarget {
        public final String title;
        public final ConfigEntry target;
        public SectionTarget(String title, ConfigEntry target) { this.title = title; this.target = target; }
    }

    public static class FlatButton extends Button {
        private boolean active;
        private final boolean isSection; 
        public FlatButton(int x, int y, int w, int h, Component msg, boolean isSection, OnPress onPress) {
            super(x, y, w, h, msg, onPress, DEFAULT_NARRATION);
            this.isSection = isSection;
        }
        public void setActive(boolean active) { this.active = active; }

        @Override public boolean isMouseOver(double mouseX, double mouseY) {
            return mouseX >= this.getX() && mouseX < this.getX() + this.width
                    && mouseY >= this.getY() && mouseY < this.getY() + this.height;
        }
        @Override public void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float pt) {
            boolean hovered = this.isHovered();
            if (active && !isSection) {
                gfx.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x55222200);
                gfx.fill(this.getX(), this.getY(), this.getX() + 2, this.getY() + this.height, 0xFFFFAA00); 
            } else if (hovered) {
                gfx.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x22FFFFFF);
            }
            if (active && isSection) {
                gfx.fill(this.getX(), this.getY() + this.height / 2 - 2, this.getX() + 2, this.getY() + this.height / 2 + 2, 0xFFFFAA00);
            }
            int textColor = active ? 0xFFFFAA : (hovered ? 0xFFFFFF : (isSection ? 0x888888 : 0xAAAAAA));
            int textX = this.getX() + (isSection ? 8 : 6); 
            net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
            int maxTextW = this.width - (isSection ? 12 : 10);
            String label = font.plainSubstrByWidth(this.getMessage().getString(), maxTextW);
            gfx.drawString(font, label, textX, this.getY() + (this.height - 8) / 2, textColor, false);
        }
    }

    public static class ConfigScreen extends Screen {
        private static final int SIDEBAR_W   = 145; 
        private static final int SIDEBAR_PAD = 4;   
        private static final int CONTENT_X   = SIDEBAR_W + 6; 

        private final Screen parent;
        private final Map<String, List<ConfigEntry>> categories;
        private final Map<String, List<SectionTarget>> sectionTargets;
        private final List<ForgeConfigSpec> specs;
        private final Map<String, Double> scrollPositions = new HashMap<>();
        private String currentCategory;
        private ConfigList listWidget;

        private EditBox searchBox;
        private String searchQuery = "";

        private String activeSectionTitle = "";
        private final Map<String, FlatButton> sectionButtons = new LinkedHashMap<>();

        public ConfigScreen(Component title, Screen parent, Map<String, List<ConfigEntry>> categories, Map<String, List<SectionTarget>> sectionTargets, List<ForgeConfigSpec> specs) {
            super(title);
            this.parent = parent;
            this.categories = categories;
            this.sectionTargets = sectionTargets;
            this.specs = specs;
            if (!categories.isEmpty()) this.currentCategory = categories.keySet().iterator().next();
        }

        @Override public void tick() {
            super.tick();
            if (this.listWidget != null && this.currentCategory != null && searchQuery.isEmpty()) {
                double scroll = this.listWidget.getScrollAmount();
                scrollPositions.put(currentCategory, scroll);

                if (sectionTargets.containsKey(currentCategory)) {
                    List<ConfigEntry> entries = categories.get(currentCategory);
                    String latest = "";
                    
                    double maxScroll = this.listWidget.getMaxScroll();
                    boolean atBottom = maxScroll > 0 && scroll >= maxScroll - 1.0;

                    if (atBottom) {
                        List<SectionTarget> targets = sectionTargets.get(currentCategory);
                        if (!targets.isEmpty()) latest = targets.get(targets.size() - 1).title;
                    } else {
                        for (int i = 0; i < entries.size(); i++) {
                            ConfigEntry e = entries.get(i);
                            if (e instanceof SectionHeaderEntry) {
                                double entryTop = i * (double) this.listWidget.getItemHeight();
                                if (entryTop <= scroll + 2) latest = e.label;
                            }
                        }
                    }
                    
                    if (!latest.equals(activeSectionTitle)) {
                        activeSectionTitle = latest;
                        for (Map.Entry<String, FlatButton> e : sectionButtons.entrySet()) {
                            e.getValue().setActive(e.getKey().equals(activeSectionTitle));
                        }
                    }
                }
            }
        }

        @Override protected void init() {
            this.clearWidgets();
            this.sectionButtons.clear();

            int contentW = this.width - CONTENT_X;
            searchBox = new EditBox(this.font, CONTENT_X + 4, 14, contentW - 8, 16, Component.literal("Search..."));
            searchBox.setMaxLength(64);
            searchBox.setHint(Component.literal("Search settings..."));
            searchBox.setValue(searchQuery);
            searchBox.setResponder(text -> {
                searchQuery = text.trim();
                rebuildList();
            });
            this.addRenderableWidget(searchBox);

            int sideY = 36; 
            for (String cat : categories.keySet()) {
                final String catCapture = cat;
                FlatButton catBtn = new FlatButton(
                        SIDEBAR_PAD, sideY, SIDEBAR_W - SIDEBAR_PAD * 2, 18,
                        Component.literal(cat), false,
                        b -> { this.currentCategory = catCapture; this.searchQuery = ""; this.searchBox.setValue(""); this.activeSectionTitle = ""; this.init(); });
                catBtn.setActive(cat.equals(this.currentCategory));
                this.addRenderableWidget(catBtn);
                sideY += 26; 

                if (cat.equals(this.currentCategory) && sectionTargets.containsKey(cat)) {
                    for (SectionTarget target : sectionTargets.get(cat)) {
                        final String secTitle = target.title;
                        FlatButton secBtn = new FlatButton(
                                SIDEBAR_PAD + 6, sideY, SIDEBAR_W - SIDEBAR_PAD * 2 - 6, 14,
                                Component.literal(target.title), true,
                                b -> {
                                    if (listWidget == null) return;
                                    int idx = this.listWidget.children().indexOf(target.target);
                                    if (idx >= 0) this.listWidget.setScrollAmount(idx * this.listWidget.getItemHeight());
                                });
                        secBtn.setActive(secTitle.equals(activeSectionTitle));
                        this.addRenderableWidget(secBtn);
                        sectionButtons.put(secTitle, secBtn);
                        sideY += 15;
                    }
                    sideY += 4; 
                }
            }

            this.listWidget = new ConfigList(this.minecraft, contentW, this.height, 34, this.height - 28, 24);
            this.listWidget.setLeftPos(CONTENT_X);
            this.addWidget(this.listWidget);
            rebuildList();

            this.addRenderableWidget(Button.builder(Component.literal("Save & Close"), b -> this.onClose())
                    .bounds(this.width / 2 - 60, this.height - 22, 120, 16).build());
        }

        private void rebuildList() {
            this.listWidget.children().clear();
            if (!searchQuery.isEmpty()) {
                String q = searchQuery.toLowerCase(java.util.Locale.ROOT);
                for (List<ConfigEntry> entries : categories.values()) {
                    for (ConfigEntry entry : entries) {
                        if (entry instanceof SectionHeaderEntry || entry instanceof SectionDescEntry
                                || entry instanceof SpacerEntry) continue;
                        boolean labelMatch = entry.label.toLowerCase(java.util.Locale.ROOT).contains(q);
                        boolean commentMatch = entry.tooltipLines.stream()
                                .anyMatch(c -> c.getString().toLowerCase(java.util.Locale.ROOT).contains(q));
                        if (labelMatch || commentMatch) this.listWidget.addEntry(entry);
                    }
                }
                this.listWidget.setScrollAmount(0); 
            } else if (currentCategory != null && categories.containsKey(currentCategory)) {
                for (ConfigEntry entry : categories.get(currentCategory)) this.listWidget.addEntry(entry);
                this.listWidget.setScrollAmount(scrollPositions.getOrDefault(currentCategory, 0.0));
            }
        }

        @Override public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(gfx);
            gfx.fill(0, 0, SIDEBAR_W, this.height, 0xAA111111);
            gfx.fill(SIDEBAR_W, 0, CONTENT_X, this.height, 0xFF282828);
            gfx.drawString(this.font, this.title, SIDEBAR_PAD + 2, 4, 0xFFFFAA00, false);
            this.listWidget.render(gfx, mouseX, mouseY, partialTick);

            if (!searchQuery.isEmpty() && this.listWidget.children().isEmpty()) {
                gfx.drawCenteredString(this.font, "No settings match \"" + searchQuery + "\"",
                        (this.width + SIDEBAR_W) / 2, this.height / 2, 0x888888);
            }

            super.render(gfx, mouseX, mouseY, partialTick);
            ConfigEntry hovered = this.listWidget.getHoveredEntry(mouseX, mouseY);
            if (hovered != null) {
                int left = this.listWidget.getRowLeft();
                int textW = this.font.width(hovered.restartReq > 0 ? "[!] " + hovered.label : hovered.label);
                if (mouseX >= left && mouseX <= left + textW + 8) {
                    List<Component> tooltip = hovered.getTooltipLines();
                    if (!tooltip.isEmpty()) gfx.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                }
            }
        }

        @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if ((keyCode == GLFW.GLFW_KEY_F) && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                this.setFocused(searchBox);
                searchBox.setFocused(true);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override public void onClose() {
            for (List<ConfigEntry> list : categories.values()) for (ConfigEntry entry : list) entry.save();
            for (ForgeConfigSpec spec : specs) spec.save();
            this.minecraft.setScreen(parent);
        }
    }

    public static class ConfigList extends ContainerObjectSelectionList<ConfigEntry> {
        private final int itemHeight;
        public ConfigList(Minecraft mc, int w, int h, int t, int b, int ih) {
            super(mc, w, h, t, b, ih);
            this.itemHeight = ih;
        }
        public int getItemHeight() { return this.itemHeight; }
        @Override public int addEntry(ConfigEntry entry) { return super.addEntry(entry); }
        @Override public int getRowWidth() { return this.width - 30; }
        @Override protected int getScrollbarPosition() { return this.getLeft() + this.width - 8; }
        @Override public int getRowLeft() { return this.getLeft() + 4; }
        
        public ConfigEntry getHoveredEntry(double mouseX, double mouseY) {
            if (mouseX >= this.getRowLeft() && mouseX <= this.getRowLeft() + this.getRowWidth()
                    && mouseY >= this.getRectangle().top() && mouseY <= this.getRectangle().bottom()) {
                int index = (int) ((mouseY - this.getRectangle().top() + this.getScrollAmount()) - 4) / this.itemHeight;
                if (index >= 0 && index < this.children().size()) return this.children().get(index);
            }
            return null;
        }
    }

    public static abstract class ConfigEntry extends ContainerObjectSelectionList.Entry<ConfigEntry> {
        public final String label;
        public final int restartReq;
        protected final String rawComment;
        protected final List<Component> tooltipLines = new ArrayList<>();
        
        protected BooleanEntry dependencyEntry;
        protected String dependencyName;

        public ConfigEntry(String label, String comment, int restartReq) {
            this.label = label;
            this.restartReq = restartReq;
            this.rawComment = comment; 
            if (restartReq == 1) tooltipLines.add(Component.literal("§e[!] Requires World Rejoin to apply changes"));
            else if (restartReq == 2) tooltipLines.add(Component.literal("§c[!] Requires Server Restart to apply changes"));
            
            if (comment != null && !comment.isEmpty()) {
                for (String line : comment.split("\n")) {
                    String trimmed = line.trim();
                    
                    if (trimmed.startsWith("Format:") || trimmed.startsWith("Example") || trimmed.startsWith("|")) {
                        break; 
                    }
                    
                    if (!trimmed.startsWith("@SUGGEST") && !trimmed.isEmpty()) {
                        tooltipLines.add(Component.literal("§7" + trimmed));
                    }
                }
            }
        }

        public ConfigEntry dependsOn(BooleanEntry entry, String name) {
            this.dependencyEntry = entry;
            this.dependencyName = name;
            return this;
        }
        
        public boolean isDisabled() {
            return dependencyEntry != null && !dependencyEntry.getCurrentValue();
        }

        public abstract void save();
        public List<Component> getTooltipLines() { 
            if (isDisabled()) return List.of(Component.literal("§cRequires '" + dependencyName + "' to be enabled."));
            return tooltipLines; 
        }

        protected void drawLabel(GuiGraphics gfx, int left, int top, int w, int h) {
            net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
            int color = isDisabled() ? 0x666666 : 0xFFFFFF;
            String prefix = "";
            if (!isDisabled()) {
                if (restartReq == 1) prefix = "§e[!] §r";
                else if (restartReq == 2) prefix = "§c[!] §r";
            } else {
                if (restartReq > 0) prefix = "[!] ";
            }
            String displayLabel = prefix + label;
            gfx.drawString(font, displayLabel, left, top + 6, color, false);
        }
    }

    public static class SectionHeaderEntry extends ConfigEntry {
        public SectionHeaderEntry(String title) { super(title, null, 0); }
        
        @Override public void save() {}
        
        @Override public void render(GuiGraphics gfx, int idx, int top, int left, int w, int h, int mX, int mY, boolean hover, float pt) {
            gfx.drawCenteredString(Minecraft.getInstance().font, "§6§l" + label, left + w / 2, top + 4, 0xFFFFFF);
        }
        
        @Override public List<? extends GuiEventListener> children() { return Collections.emptyList(); }
        @Override public List<? extends NarratableEntry> narratables() { return Collections.emptyList(); }
    }

    public static class SectionDescEntry extends ConfigEntry {
        public SectionDescEntry(String text) { super(text, null, 0); }
        
        @Override public void save() {}
        
        @Override public void render(GuiGraphics gfx, int idx, int top, int left, int w, int h, int mX, int mY, boolean hover, float pt) {
            if (!label.isEmpty()) gfx.drawCenteredString(Minecraft.getInstance().font, label, left + w / 2, top - 4, 0xAAAAAA);
            gfx.fill(left + 20, top + 8, left + w - 20, top + 9, 0x55FFFFFF);
        }
        
        @Override public List<? extends GuiEventListener> children() { return Collections.emptyList(); }
        @Override public List<? extends NarratableEntry> narratables() { return Collections.emptyList(); }
    }

    public static class SpacerEntry extends ConfigEntry {
        public SpacerEntry() { super("", null, 0); }
        
        @Override public void save() {}
        
        @Override public void render(GuiGraphics gfx, int idx, int top, int left, int w, int h, int mX, int mY, boolean hover, float pt) {
        }
        
        @Override public List<? extends GuiEventListener> children() { return Collections.emptyList(); }
        @Override public List<? extends NarratableEntry> narratables() { return Collections.emptyList(); }
    }

    public static class BooleanEntry extends ConfigEntry {
        private final ForgeConfigSpec.ConfigValue<Boolean> config;
        private boolean currentValue;
        private final Button button;

        public BooleanEntry(String label, String comment, int restartReq, ForgeConfigSpec.ConfigValue<Boolean> config) {
            super(label, comment, restartReq);
            this.config = config; 
            this.currentValue = config.get();
            this.button = Button.builder(Component.literal(this.currentValue ? "§aTrue" : "§cFalse"), b -> {
                this.currentValue = !this.currentValue; 
                b.setMessage(Component.literal(this.currentValue ? "§aTrue" : "§cFalse"));
            }).bounds(0, 0, 100, 20).build();
        }

        public boolean getCurrentValue() { return this.currentValue; }
        
        @Override public void save() { config.set(this.currentValue); }
        
        @Override public void render(GuiGraphics gfx, int idx, int top, int left, int w, int h, int mX, int mY, boolean hover, float pt) {
            this.button.active = !isDisabled();
            this.drawLabel(gfx, left, top, w, h); 
            button.setX(left + w - 100); 
            button.setY(top); 
            button.render(gfx, mX, mY, pt);
        }
        
        @Override public List<? extends GuiEventListener> children() { return List.of(button); }
        @Override public List<? extends NarratableEntry> narratables() { return List.of(button); }
    }

    public static class NumberEntry extends ConfigEntry {
        private final ForgeConfigSpec.ConfigValue<?> config;
        private final EditBox editBox;

        public NumberEntry(String label, String comment, int restartReq, ForgeConfigSpec.ConfigValue<?> config) {
            super(label, comment, restartReq);
            this.config = config; 
            this.editBox = new EditBox(Minecraft.getInstance().font, 0, 0, 100, 20, Component.empty());
            this.editBox.setMaxLength(32); 
            this.editBox.setValue(String.valueOf(config.get()));
        }

        @Override @SuppressWarnings("unchecked") public void save() {
            try {
                String val = editBox.getValue().trim();
                if (config.get() instanceof Double) ((ForgeConfigSpec.ConfigValue<Double>) config).set(Double.parseDouble(val));
                else if (config.get() instanceof Integer) ((ForgeConfigSpec.ConfigValue<Integer>) config).set(Integer.parseInt(val));
            } catch (NumberFormatException ignored) {}
        }
        
        @Override public void render(GuiGraphics gfx, int idx, int top, int left, int w, int h, int mX, int mY, boolean hover, float pt) {
            this.editBox.setEditable(!isDisabled());
            this.drawLabel(gfx, left, top, w, h); 
            editBox.setX(left + w - 100); 
            editBox.setY(top); 
            editBox.render(gfx, mX, mY, pt);
        }
        
        @Override public List<? extends GuiEventListener> children() { return List.of(editBox); }
        @Override public List<? extends NarratableEntry> narratables() { return List.of(editBox); }
    }

    public static class StringConfigEntry extends ConfigEntry {
        private final ForgeConfigSpec.ConfigValue<String> config;
        private final EditBox editBox;

        public StringConfigEntry(String label, String comment, int restartReq, ForgeConfigSpec.ConfigValue<String> config) {
            super(label, comment, restartReq);
            this.config = config; 
            this.editBox = new EditBox(Minecraft.getInstance().font, 0, 0, 150, 20, Component.empty());
            this.editBox.setMaxLength(128); 
            this.editBox.setValue(config.get() != null ? config.get() : "");
        }

        @Override public void save() { config.set(this.editBox.getValue()); }
        
        @Override public void render(GuiGraphics gfx, int idx, int top, int left, int w, int h, int mX, int mY, boolean hover, float pt) {
            this.editBox.setEditable(!isDisabled());
            this.drawLabel(gfx, left, top, w, h); 
            editBox.setX(left + w - 150); 
            editBox.setY(top); 
            editBox.render(gfx, mX, mY, pt);
        }
        
        @Override public List<? extends GuiEventListener> children() { return List.of(editBox); }
        @Override public List<? extends NarratableEntry> narratables() { return List.of(editBox); }
    }

    public static class EnumEntry<T extends Enum<T>> extends ConfigEntry {
        private final ForgeConfigSpec.EnumValue<T> config;
        private T currentValue;
        private final Button button;

        public EnumEntry(String label, String comment, int restartReq, ForgeConfigSpec.EnumValue<T> config, Class<T> enumClass) {
            super(label, comment, restartReq);
            this.config = config; 
            this.currentValue = config.get();
            T[] constants = enumClass.getEnumConstants();
            this.button = Button.builder(Component.literal(this.currentValue.name()), b -> {
                int nextIdx = (this.currentValue.ordinal() + 1) % constants.length;
                this.currentValue = constants[nextIdx]; 
                b.setMessage(Component.literal(this.currentValue.name()));
            }).bounds(0, 0, 100, 20).build();
        }

        @Override public void save() { config.set(this.currentValue); }
        
        @Override public void render(GuiGraphics gfx, int idx, int top, int left, int w, int h, int mX, int mY, boolean hover, float pt) {
            this.button.active = !isDisabled();
            this.drawLabel(gfx, left, top, w, h); 
            button.setX(left + w - 100); 
            button.setY(top); 
            button.render(gfx, mX, mY, pt);
        }
        
        @Override public List<? extends GuiEventListener> children() { return List.of(button); }
        @Override public List<? extends NarratableEntry> narratables() { return List.of(button); }
    }

    public static class ListEntry extends ConfigEntry {
        private final Button button;

        public ListEntry(String label, String comment, int restartReq, ForgeConfigSpec.ConfigValue<List<? extends String>> config) {
            super(label, comment, restartReq);
            this.button = Button.builder(Component.literal("Edit List..."), b -> {
                Minecraft.getInstance().setScreen(new SubListScreen(label, config, comment, Minecraft.getInstance().screen));
            }).bounds(0, 0, 100, 20).build();
        }

        @Override public void save() { }
        
        @Override public void render(GuiGraphics gfx, int idx, int top, int left, int w, int h, int mX, int mY, boolean hover, float pt) {
            this.button.active = !isDisabled();
            this.drawLabel(gfx, left, top, w, h); 
            button.setX(left + w - 100); 
            button.setY(top); 
            button.render(gfx, mX, mY, pt);
        }
        
        @Override public List<? extends GuiEventListener> children() { return List.of(button); }
        @Override public List<? extends NarratableEntry> narratables() { return List.of(button); }
    }

    public static class SubListScreen extends Screen {
        private final ForgeConfigSpec.ConfigValue<List<? extends String>> config;
        private final Screen parent;
        public final String formatGuideText;
        public final SchemaInfo schema;
        private SubListWidget listWidget;

        public SubListScreen(String title, ForgeConfigSpec.ConfigValue<List<? extends String>> config, String formatGuideText, Screen parent) {
            super(Component.literal(title));
            this.config = config;
            this.formatGuideText = formatGuideText;
            this.schema = SchemaInfo.parse(formatGuideText);
            this.parent = parent;
        }

        @Override protected void init() {
            this.clearWidgets();
            this.listWidget = new SubListWidget(this.minecraft, this.width, this.height, 45, this.height - 35, 25);
            for (String s : config.get()) this.listWidget.addEntry(new StringEntry(s, this.listWidget, this));
            this.addWidget(this.listWidget);

            this.addRenderableWidget(Button.builder(Component.literal("Add New Row"), b -> {
                Minecraft.getInstance().setScreen(new EntryEditScreen(this, "", this.schema, this.formatGuideText, val -> {
                    this.listWidget.addEntry(new StringEntry(val, this.listWidget, this));
                    this.listWidget.setScrollAmount(this.listWidget.getMaxScroll());
                    this.saveListToConfig();
                }));
            }).bounds(this.width / 2 - 155, this.height - 28, 150, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("Save & Back"), b -> {
                saveListToConfig();
                this.onClose();
            }).bounds(this.width / 2 + 5, this.height - 28, 150, 20).build());
        }

        public void saveListToConfig() {
            List<String> newValues = new ArrayList<>();
            for (StringEntry entry : this.listWidget.children()) {
                if (!entry.getValue().trim().isEmpty()) newValues.add(entry.getValue());
            }
            ((ForgeConfigSpec.ConfigValue<List<String>>) (Object) config).set(newValues);
        }

        @Override public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(gfx);
            this.listWidget.render(gfx, mouseX, mouseY, partialTick);
            gfx.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
            super.render(gfx, mouseX, mouseY, partialTick);
        }

        @Override public void onClose() { this.minecraft.setScreen(parent); }
    }

    public static class SubListWidget extends ContainerObjectSelectionList<StringEntry> {
        private final int itemHeight;
        public SubListWidget(Minecraft mc, int w, int h, int t, int b, int ih) { super(mc, w, h, t, b, ih); this.itemHeight = ih; }
        public int getItemHeight() { return this.itemHeight; }
        @Override public int addEntry(StringEntry entry) { return super.addEntry(entry); }
        @Override public boolean removeEntry(StringEntry entry) { 
            boolean rem = super.removeEntry(entry); 
            if(this.minecraft.screen instanceof SubListScreen screen) screen.saveListToConfig();
            return rem; 
        }
        @Override public int getRowWidth() { return this.width - 80; }
        @Override protected int getScrollbarPosition() { return this.width - 20; }
        @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            this.setScrollAmount(this.getScrollAmount() - delta * this.itemHeight * 3.0); return true;
        }
    }

    public static class StringEntry extends ContainerObjectSelectionList.Entry<StringEntry> {
        private final EditBox editBox;
        private final Button removeButton;
        private final Button wizardButton;

        public StringEntry(String value, SubListWidget parent, SubListScreen parentScreen) {
            this.editBox = new EditBox(Minecraft.getInstance().font, 0, 0, parent.getRowWidth() - 55, 20, Component.empty());
            this.editBox.setMaxLength(2000); this.editBox.setValue(value);
            
            this.wizardButton = Button.builder(Component.literal("✎"), b -> {
                Minecraft.getInstance().setScreen(new EntryEditScreen(parentScreen, this.editBox.getValue(), parentScreen.schema, parentScreen.formatGuideText, val -> {
                    this.editBox.setValue(val);
                    parentScreen.saveListToConfig();
                }));
            }).bounds(0, 0, 20, 20).build();
            
            this.removeButton = Button.builder(Component.literal("X"), b -> parent.removeEntry(this))
                    .bounds(0, 0, 20, 20).build();
        }
        public String getValue() { return this.editBox.getValue(); }
        @Override public void render(GuiGraphics gfx, int idx, int top, int left, int w, int h, int mX, int mY, boolean hover, float pt) {
            this.editBox.setX(left); this.editBox.setY(top); this.editBox.render(gfx, mX, mY, pt);
            this.wizardButton.setX(left + w - 45); this.wizardButton.setY(top); this.wizardButton.render(gfx, mX, mY, pt);
            this.removeButton.setX(left + w - 20); this.removeButton.setY(top); this.removeButton.render(gfx, mX, mY, pt);
        }
        @Override public List<? extends GuiEventListener> children() { return List.of(editBox, wizardButton, removeButton); }
        @Override public List<? extends NarratableEntry> narratables() { return List.of(editBox, wizardButton, removeButton); }
    }

    public static class EntryEditScreen extends Screen {
        private final Screen parent;
        private final Consumer<String> onSave;
        private final String formatGuideText;
        private final SchemaInfo schema;
        
        private EditBox editBox;
        private HelpTextWidget helpWidget;
        private boolean showHelp = false;
        private String validationError = "";
        private SuggestionBox suggestionBox;
        private String savedBoxValue;

        public EntryEditScreen(Screen parent, String initialValue, SchemaInfo schema, String formatGuideText, Consumer<String> onSave) {
            super(Component.literal("Edit Entry"));
            this.parent = parent;
            this.schema = schema;
            this.formatGuideText = formatGuideText;
            this.onSave = onSave;
            this.savedBoxValue = initialValue;
        }

        @Override 
        protected void init() {
            this.clearWidgets();
            int boxWidth = Math.min(400, this.width - 40);
            
            this.editBox = new EditBox(Minecraft.getInstance().font, this.width / 2 - boxWidth / 2, 45, boxWidth, 20, Component.empty());
            this.editBox.setMaxLength(2000);
            this.editBox.setValue(this.savedBoxValue);
            this.addRenderableWidget(this.editBox); 
            this.setInitialFocus(this.editBox);
            
            this.suggestionBox = new SuggestionBox(this.editBox, this.schema, this.font);
            this.addWidget(suggestionBox); 

            this.helpWidget = new HelpTextWidget(this.minecraft, this.width, this.height, 105, this.height - 40, 14);
            if (formatGuideText != null) {
                List<Float> colWidths = new ArrayList<>();
                boolean headerRow = true;
                for (String line : formatGuideText.split("\n")) {
                    line = line.trim();
                    if (line.startsWith("@SUGGEST")) continue;
                    
                    if (line.isEmpty()) {
                        colWidths.clear(); 
                        headerRow = true;
                        this.helpWidget.addEntry(new GuideLineEntry(Component.empty().getVisualOrderText()));
                        continue;
                    }

                    if (line.startsWith("|")) {
                        String[] cols = line.substring(1, line.length() - (line.endsWith("|") ? 1 : 0)).split("\\|");
                        if (line.contains("---")) {
                            headerRow = false;
                            this.helpWidget.addEntry(new TableSeparatorEntry(new ArrayList<>(colWidths)));
                            continue;
                        }
                        if (colWidths.isEmpty()) {
                            for (int i = 0; i < cols.length; i++) colWidths.add(1.0f / cols.length);
                        }
                        
                        String[] safeCols = new String[colWidths.size()];
                        for(int i = 0; i < colWidths.size(); i++) safeCols[i] = i < cols.length ? cols[i].trim() : "";
                        
                        int totalWidth = this.helpWidget.getRowWidth() - 20;
                        List<List<net.minecraft.util.FormattedCharSequence>> wrappedCols = new ArrayList<>();
                        int maxLines = 1;
                        for(int i = 0; i < safeCols.length; i++) {
                            int colW = (int)(totalWidth * colWidths.get(i)) - 6;
                            List<net.minecraft.util.FormattedCharSequence> wrapped = this.font.split(Component.literal(safeCols[i]), Math.max(10, colW));
                            wrappedCols.add(wrapped);
                            maxLines = Math.max(maxLines, wrapped.size());
                        }
                        
                        for(int l = 0; l < maxLines; l++) {
                            net.minecraft.util.FormattedCharSequence[] rowLine = new net.minecraft.util.FormattedCharSequence[colWidths.size()];
                            for(int c = 0; c < colWidths.size(); c++) {
                                rowLine[c] = l < wrappedCols.get(c).size() ? wrappedCols.get(c).get(l) : null;
                            }
                            this.helpWidget.addEntry(new TableRowEntry(rowLine, new ArrayList<>(colWidths), headerRow && l == 0, this.schema));
                        }
                        continue;
                    }

                    boolean isHeader = line.startsWith("Format:") || line.startsWith("Types:") || 
                                       line.startsWith("Examples:") || line.startsWith("Priority") || line.startsWith("Note:");
                    String displayLine = isHeader ? "§e" + line : "§7" + line;
                    for (net.minecraft.util.FormattedCharSequence seq : this.font.split(Component.literal(displayLine), this.helpWidget.getRowWidth() - 15)) {
                        this.helpWidget.addEntry(new GuideLineEntry(seq));
                    }
                }
            }

            if (showHelp) {
                this.addWidget(this.helpWidget);
            }

            this.addRenderableWidget(Button.builder(Component.literal(showHelp ? "Hide Format Guide" : "View Format Guide"), b -> {
                this.savedBoxValue = this.editBox.getValue();
                int savedCursorPos = this.editBox.getCursorPosition();
                this.showHelp = !this.showHelp;
                this.init(); 
                this.editBox.setCursorPosition(savedCursorPos);
            }).bounds(this.width - 125, 10, 115, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> this.onClose())
                    .bounds(this.width / 2 - 155, this.height - 28, 150, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("Save & Add"), b -> {
                String error = schema.validate(this.editBox.getValue());
                if (error != null) {
                    this.validationError = error;
                } else {
                    onSave.accept(this.editBox.getValue());
                    this.onClose();
                }
            }).bounds(this.width / 2 + 5, this.height - 28, 150, 20).build());
        }

        private String getFriendlyTypeDesc(String type) {
            if (type.equals("ITEM")) return "an Item Registry Name (e.g. minecraft:apple)";
            if (type.equals("BLOCK")) return "a Block Registry Name (e.g. minecraft:stone)";
            if (type.equals("EFFECT")) return "a Status Effect Registry Name (e.g. minecraft:speed)";
            if (type.equals("ATTRIBUTE")) return "an Attribute Registry Name (e.g. minecraft:generic.attack_damage)";
            if (type.equals("ENTITY")) return "an Entity Registry Name (e.g. minecraft:zombie)";
            if (type.equals("FLOAT")) return "a Decimal Number (e.g. 1.5)";
            if (type.equals("INT")) return "a Whole Number (e.g. 5)";
            if (type.equals("COLOR")) return "a Decimal Integer (e.g. 16711680 for Red)";
            if (type.equals("ICON")) return "a Custom UI Emoji symbol or 'none'";
            if (type.startsWith("ENUM(")) return "Select an option from the dropdown menu.";
            if (type.equals("ANY")) return "Any string value or match parameter";
            return type;
        }
        
        @Override 
        public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
            if (this.getFocused() == this.suggestionBox) {
                this.setFocused(this.editBox);
                this.editBox.setFocused(true);
            }
            
            this.renderBackground(gfx);
            if (showHelp) this.helpWidget.render(gfx, mouseX, mouseY, partialTick);
            super.render(gfx, mouseX, mouseY, partialTick);
            
            gfx.drawCenteredString(this.font, "§eEdit Config Entry", this.width / 2, 20, 0xFFFFFF);
            
            this.suggestionBox.updateSuggestions();
            int activeArg = suggestionBox.getActiveArgIndex();
            
            int textY = 70;
            if (!showHelp && this.suggestionBox.isActive()) {
                int visibleCount = Math.min(this.suggestionBox.currentSuggestions.size(), this.suggestionBox.maxVisible);
                textY = 65 + (visibleCount * 14) + 10;
            }
            
            int WINDOW_SIZE = 5;
            int maxDefined = schema.strictTypes.isEmpty() ? -1 : Collections.max(schema.strictTypes.keySet());
            int displayMax = schema.hasVarargs ? Math.max(maxDefined, activeArg + 1) : maxDefined;

            String[] currentParts = this.editBox.getValue().split("[=,;]", -1);
            
            if (displayMax >= 0) {
                int startIdx = Math.max(0, activeArg - 2);
                int endIdx = Math.min(displayMax, startIdx + WINDOW_SIZE - 1);
                
                if (endIdx - startIdx < WINDOW_SIZE - 1) {
                    startIdx = Math.max(0, endIdx - WINDOW_SIZE + 1);
                }
                
                List<String> activeNames = new ArrayList<>();
                List<String> activeSubtexts = new ArrayList<>();
                List<Integer> activeIndices = new ArrayList<>();
                
                for (int i = startIdx; i <= endIdx; i++) { 
                    String type = schema.getTypeForIndex(i, this.editBox.getValue());
                    if (type != null && !type.equals("NONE")) {
                        int chainIdx = schema.getChainIndex(this.editBox.getValue(), i);
                        String rawName = schema.getNameForIndex(i, this.editBox.getValue()).replace("{C}", String.valueOf(chainIdx));
                        String sub = schema.getSubtextForIndex(i, this.editBox.getValue()).replace("{C}", String.valueOf(chainIdx));

                        boolean isUntouched = (i > activeArg) || (i == activeArg && (i >= currentParts.length || currentParts[i].trim().isEmpty()));
                        boolean isStarter = type.startsWith("ENUM(") && 
                            (type.contains("TICK") || type.contains("INSTANT") || type.contains("START") || type.contains("PASSIVE_OVER"));
                        
                        if (schema.hasVarargs && i > maxDefined && isUntouched && isStarter) {
                            rawName = "[OPTIONAL]";
                            sub = "";
                        }
          
                        if (rawName.equalsIgnoreCase("Optional") || rawName.equalsIgnoreCase("[OPTIONAL]")) rawName = "[OPTIONAL]";

                        activeNames.add(rawName);
                        activeSubtexts.add(sub.isEmpty() ? "" : "(" + sub + ")");
                        activeIndices.add(i);
                    }
                }
                
                if (!activeNames.isEmpty()) {
                    int actualStart = activeIndices.get(0);
                    int actualEnd = activeIndices.get(activeIndices.size() - 1);

                    int totalWidth = font.width("Format: ");
                    if (actualStart > 0) {
                        char preDelim = schema.getDelimiterAfter(actualStart - 1);
                        totalWidth += font.width("... " + preDelim + " ");
                    }
                    
                    for(int i = 0; i < activeNames.size(); i++) {
                        totalWidth += Math.max(font.width(activeNames.get(i)), font.width(activeSubtexts.get(i)));
                        if(i < activeNames.size() - 1) {
                            char delim = schema.getDelimiterAfter(activeIndices.get(i));
                            totalWidth += font.width(" " + delim + " ");
                        }
                    }
                    
                    boolean hasNextArg = schema.getTypeForIndex(actualEnd + 1, this.editBox.getValue()) != null && !schema.getTypeForIndex(actualEnd + 1, this.editBox.getValue()).equals("NONE");
                    if (hasNextArg) {
                        char postDelim = schema.getDelimiterAfter(actualEnd);
                        totalWidth += font.width(" " + postDelim + " ...");
                    }
                    
                    int drawX = (this.width / 2) - (totalWidth / 2);
                    gfx.drawString(this.font, "§8Format: ", drawX, textY, 0xFFFFFF, false);
                    drawX += font.width("Format: ");
      
                    if (actualStart > 0) {
                        char preDelim = schema.getDelimiterAfter(actualStart - 1);
                        gfx.drawString(this.font, "§7... §8" + preDelim + " ", drawX, textY, 0xFFFFFF, false);
                        drawX += font.width("... " + preDelim + " ");
                    }
                    
                    for(int i = 0; i < activeNames.size(); i++) {
                        String n = activeNames.get(i);
                        String s = activeSubtexts.get(i);
                        int globalIndex = activeIndices.get(i);
                        
                        int wName = font.width(n);
                        int wSub = font.width(s);
                        int colW = Math.max(wName, wSub);
                        int nameX = drawX + (colW - wName) / 2;
                        
                        String nameColor = (globalIndex == activeArg ? "§e" : "§7");
                        if (n.equals("[OPTIONAL]")) nameColor = (globalIndex == activeArg ? "§6" : "§c");
                        gfx.drawString(this.font, nameColor + n, nameX, textY, 0xFFFFFF, false);
                        
                        if (!s.isEmpty()) {
                            int subX = drawX + (colW - wSub) / 2;
                            gfx.drawString(this.font, "§8" + s, subX, textY + 10, 0xFFFFFF, false);
                        }
                        
                        drawX += colW;
                        if(i < activeNames.size() - 1) {
                            char delim = schema.getDelimiterAfter(globalIndex);
                            gfx.drawString(this.font, " §8" + delim + " ", drawX, textY, 0xFFFFFF, false);
                            drawX += font.width(" " + delim + " ");
                        }
                    }
                    
                    if (hasNextArg) {
                        char postDelim = schema.getDelimiterAfter(actualEnd);
                        gfx.drawString(this.font, " §8" + postDelim + " §7...", drawX, textY, 0xFFFFFF, false);
                    }
                } else {
                    gfx.drawCenteredString(this.font, "§8Format: " + (schema.baseFormat.isEmpty() ? "..." : schema.baseFormat), this.width / 2, textY, 0xFFFFFF);
                }
            } else {
                gfx.drawCenteredString(this.font, "§8Format: " + (schema.baseFormat.isEmpty() ? "..." : schema.baseFormat), this.width / 2, textY, 0xFFFFFF);
            }
            
            int statusY = textY + 25;
            if (!validationError.isEmpty()) {
                gfx.drawCenteredString(this.font, "§cError: " + validationError, this.width / 2, statusY, 0xFFFFFF);
            } else {
                String activeType = schema.getTypeForIndex(activeArg, this.editBox.getValue());
                if (activeType == null || activeType.equals("NONE")) {
                    gfx.drawCenteredString(this.font, "§cExpects: No further arguments allowed.", this.width / 2, statusY, 0xFFFFFF);
                } else {
                    String friendly = getFriendlyTypeDesc(activeType);
                    String activeName = schema.getNameForIndex(activeArg, this.editBox.getValue());
                    
                    boolean isActiveUntouched = (activeArg >= currentParts.length || currentParts[activeArg].trim().isEmpty());
                    boolean isStarterActive = activeType.startsWith("ENUM(") && 
                        (activeType.contains("TICK") || activeType.contains("INSTANT") || activeType.contains("START") || activeType.contains("PASSIVE_OVER"));
                    
                    if (schema.hasVarargs && activeArg > maxDefined && isActiveUntouched && isStarterActive) {
                        activeName = "[OPTIONAL]";
                    } else {
                        activeName = activeName.replace("{C}", String.valueOf(schema.getChainIndex(this.editBox.getValue(), activeArg)));
                    }
                   
                    if (activeName.equalsIgnoreCase("Optional")) activeName = "[OPTIONAL]";
                    
                    gfx.drawCenteredString(this.font, "§7" + activeName + " Expects: §a" + friendly, this.width / 2, statusY, 0xFFFFFF);
                }
            }

            if (!showHelp) this.suggestionBox.render(gfx, mouseX, mouseY, partialTick);
        }

        @Override 
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (this.showHelp) {
                return this.helpWidget.mouseScrolled(mouseX, mouseY, delta);
            }
            if (this.suggestionBox.isActive()) {
                return this.suggestionBox.mouseScrolled(mouseX, mouseY, delta);
            }
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        @Override 
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            this.validationError = ""; 
            this.savedBoxValue = this.editBox.getValue();

            if (!this.showHelp && this.suggestionBox.isActive() && this.suggestionBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true; 
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override 
        public void onClose() { this.minecraft.setScreen(parent); }
    }
    
    public static class SchemaInfo {
        public String baseFormat = "";
        public String note = ""; 
        public boolean hasVarargs = false;
        public String varargType = "NONE", varargName = "[OPTIONAL]", varargSubtext = "";
        
        public Map<Integer, String> strictTypes = new HashMap<>(); 
        public Map<Integer, String> names = new HashMap<>();
        public Map<Integer, String> subtexts = new HashMap<>();       
        public List<Character> delimiters = new ArrayList<>();

        public static class Suggestion {
            public final String value, description;
            public final Integer colorPreview;
            public Suggestion(String v, String d, Integer c) { this.value = v; this.description = d; this.colorPreview = c; }
        }

        public static SchemaInfo parse(String text) {
            SchemaInfo schema = new SchemaInfo();
            if (text == null) return schema;
            for (String line : text.split("\n")) {
                line = line.trim();
                if (line.toLowerCase().startsWith("format:")) {
                    schema.baseFormat = line.substring(7).trim();
                    for (char c : schema.baseFormat.toCharArray()) {
                        if (c == '=' || c == ',' || c == ';') {
                            schema.delimiters.add(c);
                        }
                    }
                }
                if (line.toUpperCase().startsWith("NOTE:")) schema.note = line;
                if (line.startsWith("@SUGGEST")) {
                    try {
                        int endBracket = line.indexOf(']');
                        String idxStr = line.substring(9, endBracket);
                        String raw = line.substring(line.indexOf(":") + 1).trim();
                        
                        String namePart = raw;
                        String typePart = raw;
                        if (raw.contains("|")) {
                            String[] split = raw.split("\\|");
                            namePart = split[0].trim();
                            typePart = split[1].trim();
                        }
                        
                        String name = namePart;
                        String subtext = "";
                        if (namePart.contains("//")) {
                            String[] nameSplit = namePart.split("//");
                            name = nameSplit[0].trim();
                            subtext = nameSplit[1].trim();
                        }
                        
                        if (idxStr.equals("*")) {
                            schema.hasVarargs = true;
                            schema.varargType = typePart;
                            schema.varargName = name;
                            schema.varargSubtext = subtext;
                        } else {
                            int idx = Integer.parseInt(idxStr);
                            schema.strictTypes.put(idx, typePart);
                            schema.names.put(idx, name);
                            schema.subtexts.put(idx, subtext);
                        }
                    } catch (Exception ignored) {}
                }
            }
            return schema;
        }

        public char getDelimiterAfter(int argIndex) {
            if (delimiters.isEmpty()) return ';'; 
            if (argIndex >= 0 && argIndex < delimiters.size()) return delimiters.get(argIndex);
            return delimiters.get(delimiters.size() - 1); 
        }

        public int getChainIndex(String fullString, int currentIdx) {
            int count = 0;
            for (int i = 0; i <= currentIdx; i++) {
                String type = getTypeForIndex(i, fullString);
                if (type != null && type.startsWith("ENUM(") && 
                    (type.contains("TICK") || type.contains("INSTANT") || type.contains("START") || type.contains("PASSIVE_OVER"))) {
                    count++;
                }
            }
            return Math.max(1, count);
        }
        
        public String resolveDynamic(String base, String fullString, int currentIdx) {
            if (base != null && base.startsWith("DYNAMIC(")) {
                String[] parts = fullString.split("[=,;]", -1);
                String inner = base.substring(8, base.length() - 1); 
           
                for(String condition : inner.split("#")) { 
                    String[] condSplit = condition.split("=");
                    if (condSplit.length != 2) continue;
                    
                    String leftSide = condSplit[0].trim();
                    if (leftSide.equals("ANY")) return condSplit[1].trim();
                    
                    String[] leftSplit = leftSide.split(":");
                    if (leftSplit.length != 2) continue;
                    
                    int checkIdx = Integer.parseInt(leftSplit[0].trim());
                    String checkVal = leftSplit[1].trim();
                    
                    if (checkVal.equals("ANY")) return condSplit[1].trim();
                    int actualCheckIdx = checkIdx < 0 ? (currentIdx + checkIdx) : checkIdx;
                    if (actualCheckIdx >= 0 && actualCheckIdx < parts.length) {
                        if (parts[actualCheckIdx].trim().equals(checkVal)) {
                            return condSplit[1].trim();
                        }
                    }
                }
                return "NONE";
            }
            return base != null ? base : "NONE";
        }

        public String getNameForIndex(int idx, String fullString) { 
            int maxKey = strictTypes.isEmpty() ? -1 : Collections.max(strictTypes.keySet());
            if (strictTypes.containsKey(idx)) return resolveDynamic(names.get(idx), fullString, idx);
            if (hasVarargs && idx > maxKey) return resolveDynamic(varargName, fullString, idx);
            return "Arg " + (idx + 1);
        }

        public String getSubtextForIndex(int idx, String fullString) { 
            int maxKey = strictTypes.isEmpty() ? -1 : Collections.max(strictTypes.keySet());
            if (strictTypes.containsKey(idx)) return resolveDynamic(subtexts.get(idx), fullString, idx);
            if (hasVarargs && idx > maxKey) return resolveDynamic(varargSubtext, fullString, idx);
            return ""; 
        }

        public String getTypeForIndex(int idx, String fullString) {
            int maxKey = strictTypes.isEmpty() ? -1 : Collections.max(strictTypes.keySet());
            if (strictTypes.containsKey(idx)) return resolveDynamic(strictTypes.get(idx), fullString, idx);
            if (hasVarargs && idx > maxKey) return resolveDynamic(varargType, fullString, idx);
            return "NONE";
        }

        public String validate(String input) {
            String[] parts = input.split("[=,;]", -1);
            int maxKey = strictTypes.isEmpty() ? -1 : Collections.max(strictTypes.keySet());
            
            for (int i = 0; i < parts.length; i++) {
                String p = parts[i].trim();
                String type = getTypeForIndex(i, input);
                
                if (type == null || type.equals("NONE")) {
                    if (!p.isEmpty() || i < parts.length - 1) return "Too many arguments! Argument " + (i + 1) + " is not required here.";
                    continue;
                }

                if (p.isEmpty()) return "Argument " + (i + 1) + " (" + getNameForIndex(i, input) + ") cannot be empty.";
                if (type.equals("ITEM")) {
                    if (!ForgeRegistries.ITEMS.containsKey(new ResourceLocation(p))) return "'" + p + "' is not a valid Item ID.";
                } else if (type.equals("BLOCK")) {
                    if (!ForgeRegistries.BLOCKS.containsKey(new ResourceLocation(p))) return "'" + p + "' is not a valid Block ID.";
                } else if (type.equals("EFFECT")) {
                    if (!ForgeRegistries.MOB_EFFECTS.containsKey(new ResourceLocation(p))) return "'" + p + "' is not a valid Effect ID.";
                } else if (type.equals("ATTRIBUTE")) {
                    if (!ForgeRegistries.ATTRIBUTES.containsKey(new ResourceLocation(p))) return "'" + p + "' is not a valid Attribute ID.";
                } else if (type.equals("FLOAT")) {
                    try { Float.parseFloat(p);
                    } catch (Exception e) { return "'" + p + "' must be a number.";
                    }
                } else if (type.equals("INT")) {
                    try { Integer.parseInt(p);
                    } catch (Exception e) { return "'" + p + "' must be a whole number.";
                    }
                } else if (type.equals("COLOR")) {
                    try { Integer.parseInt(p);
                    } catch (Exception e) { return "'" + p + "' must be an Integer Color Code.";
                    }
                } else if (type.startsWith("ENUM(")) {
                    String allowed = type.substring(5, type.length() - 1);
                    boolean found = false;
                    for (String a : allowed.split(",")) {
                        String expected = a.split(":")[0].trim();
                        if (expected.equals(p)) found = true;
                    }
                    if (!found) return "'" + p + "' is not a valid predefined option.";
                }
            }
            
            for (int i = parts.length; i <= maxKey; i++) {
                String type = getTypeForIndex(i, input);
                if (type != null && !type.equals("NONE")) {
                    return "Missing required argument: " + getNameForIndex(i, input);
                }
            }
            
            return null;
        }
        
        public List<Suggestion> getSuggestions(String type, String currentText) {
            List<Suggestion> list = new ArrayList<>();
            if (type.equals("ITEM")) {
                ForgeRegistries.ITEMS.getEntries().forEach(entry -> {
                    net.minecraft.world.item.Item item = entry.getValue();
                    String desc = item instanceof net.minecraft.world.item.BlockItem ? "Block" : "Item";
                    list.add(new Suggestion(entry.getKey().location().toString(), desc, null));
                });
            }
            else if (type.equals("BLOCK")) ForgeRegistries.BLOCKS.getKeys().forEach(k -> list.add(new Suggestion(k.toString(), "Block", null)));
            else if (type.equals("TAG")) ForgeRegistries.ITEMS.tags().getTagNames().forEach(tag -> list.add(new Suggestion(tag.location().toString(), "Item Tag", null)));
            else if (type.equals("EFFECT")) ForgeRegistries.MOB_EFFECTS.getKeys().forEach(k -> list.add(new Suggestion(k.toString(), "Effect", null)));
            else if (type.equals("ATTRIBUTE")) ForgeRegistries.ATTRIBUTES.getKeys().forEach(k -> list.add(new Suggestion(k.toString(), "Attribute", null)));
            else if (type.equals("ENTITY")) ForgeRegistries.ENTITY_TYPES.getKeys().forEach(k -> list.add(new Suggestion(k.toString(), "Entity", null)));
            else if (type.equals("COLOR")) {
                list.add(new Suggestion("16711680", "Red", 16711680)); list.add(new Suggestion("65280", "Green", 65280)); list.add(new Suggestion("255", "Blue", 255));
                list.add(new Suggestion("16776960", "Yellow", 16776960)); list.add(new Suggestion("16777215", "White", 16777215)); list.add(new Suggestion("0", "Black", 0));
                list.add(new Suggestion("8421504", "Gray", 8421504)); list.add(new Suggestion("5592405", "Dark Gray", 5592405)); list.add(new Suggestion("11184810", "Light Gray", 11184810));
                list.add(new Suggestion("16747520", "Orange", 16747520)); list.add(new Suggestion("11141375", "Purple", 11141375)); list.add(new Suggestion("16711935", "Magenta", 16711935));
                list.add(new Suggestion("16738740", "Pink", 16738740)); list.add(new Suggestion("65535", "Cyan", 65535)); list.add(new Suggestion("32768", "Lime", 32768));
                list.add(new Suggestion("9127187", "Brown", 9127187)); list.add(new Suggestion("16755200", "Gold", 16755200));
            } else if (type.equals("ICON")) {
                list.add(new Suggestion("none", "Disable", null)); list.add(new Suggestion("💧", "Water", null)); list.add(new Suggestion("🔥", "Fire", null));
                list.add(new Suggestion("❄", "Cold", null)); list.add(new Suggestion("💀", "Death", null)); list.add(new Suggestion("☣", "Poison", null));
                list.add(new Suggestion("🛡", "Defense", null)); list.add(new Suggestion("⚡", "Energy", null)); list.add(new Suggestion("❤", "Health", null));
                list.add(new Suggestion("🍗", "Food", null)); list.add(new Suggestion("🏹", "Ranged", null));
                list.add(new Suggestion("🏃", "Sprint", null)); list.add(new Suggestion("🏊", "Swim", null));
                list.add(new Suggestion("💤", "Fatigue", null));
            } else if (type.startsWith("ENUM(")) {
                String inner = type.substring(5, type.length() - 1);
                for (String a : inner.split(",")) {
                    String[] parts = a.split(":", 2);
                    String val = parts[0].trim();
                    String desc = parts.length > 1 ? parts[1].trim() : "";
                    list.add(new Suggestion(val, desc, null));
                }
            }
            
            String lower = currentText.toLowerCase();
            return list.stream()
                       .filter(s -> s.value.toLowerCase().contains(lower) || s.description.toLowerCase().contains(lower))
                       .sorted(Comparator.comparing(s -> s.value))
                       .collect(Collectors.toList());
        }
    }

    public static class SuggestionBox implements GuiEventListener, NarratableEntry {
        private final EditBox editBox;
        private final SchemaInfo schema;
        private final net.minecraft.client.gui.Font font;
        private List<SchemaInfo.Suggestion> currentSuggestions = new ArrayList<>();
        private int selectedIndex = 0;
        private int activeArgIndex = 0;
        private String currentArgText = "";
        
        private int scrollOffset = 0;
        private final int maxVisible = 8;
        
        private double lastMouseX = -1;
        private double lastMouseY = -1;
        
        public SuggestionBox(EditBox editBox, SchemaInfo schema, net.minecraft.client.gui.Font font) {
            this.editBox = editBox; this.schema = schema; this.font = font;
        }

        public int getActiveArgIndex() { return activeArgIndex; }
        public boolean isActive() { return !currentSuggestions.isEmpty(); }

        public void updateSuggestions() {
            String text = editBox.getValue();
            int cursor = editBox.getCursorPosition();
            String beforeCursor = text.substring(0, Math.min(cursor, text.length()));
            
            this.activeArgIndex = beforeCursor.length() - beforeCursor.replaceAll("[=,;]", "").length();
            String[] parts = text.split("[=,;]", -1);
            if (activeArgIndex < parts.length) {
                this.currentArgText = parts[activeArgIndex].trim();
            } else {
                this.currentArgText = "";
            }
            
            String type = schema.getTypeForIndex(activeArgIndex, text);
            if (type.isEmpty() || type.equals("ANY") || type.equals("FLOAT") || type.equals("INT")) {
                currentSuggestions.clear();
            } else {
                List<SchemaInfo.Suggestion> newSug = schema.getSuggestions(type, currentArgText);
                boolean differs = currentSuggestions.size() != newSug.size();
                if(!differs) {
                    for(int i=0; i<newSug.size(); i++) {
                        if(!currentSuggestions.get(i).value.equals(newSug.get(i).value)) differs = true;
                    }
                }
                
                if (differs) {
                    currentSuggestions = newSug;
                    selectedIndex = 0;
                    scrollOffset = 0;
                }
            }
        }

        @Override 
        public boolean mouseScrolled(double mX, double mY, double delta) {
            if(currentSuggestions.size() > maxVisible) {
                scrollOffset = (int) Math.max(0, Math.min(currentSuggestions.size() - maxVisible, scrollOffset - delta));
                return true;
            }
            return false;
        }

        @Override 
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (!isActive()) return false;
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                selectedIndex = Math.min(currentSuggestions.size() - 1, selectedIndex + 1);
                if (selectedIndex >= scrollOffset + maxVisible) scrollOffset++;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP) {
                selectedIndex = Math.max(0, selectedIndex - 1);
                if (selectedIndex < scrollOffset) scrollOffset--;
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_TAB) {
                applySuggestion();
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!isActive() || button != 0) return false; 
            int x = editBox.getX();
            int y = editBox.getY() + editBox.getHeight();
            int w = editBox.getWidth();
            int visibleCount = Math.min(currentSuggestions.size(), maxVisible);
            int h = visibleCount * 14 + 4;
            
            if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
                int clickedIndex = (int) ((mouseY - y - 2) / 14);
                if (clickedIndex >= 0 && clickedIndex < visibleCount) {
                    selectedIndex = clickedIndex + scrollOffset;
                    applySuggestion();
                    return true; 
                }
            }
            return false;
        }
        
        private void applySuggestion() {
            if (currentSuggestions.isEmpty()) return;
            String sug = currentSuggestions.get(selectedIndex).value;
            String text = editBox.getValue();
            
            int delimiterCount = 0;
            int argStart = 0;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '=' || c == ',' || c == ';') {
                    if (delimiterCount == activeArgIndex) break;
                    delimiterCount++;
                    argStart = i + 1;
                }
            }
            
            int argEnd = text.length();
            for (int i = argStart; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '=' || c == ',' || c == ';') {
                    argEnd = i;
                    break;
                }
            }
            
            String fullSoFar = text.substring(0, argStart) + sug + text.substring(argEnd);
            
            String nextType = schema.getTypeForIndex(activeArgIndex + 1, fullSoFar);
            boolean hasNext = nextType != null && !nextType.equals("NONE");
            int totalDelimiters = text.length() - text.replaceAll("[=,;]", "").length();
            
            if (activeArgIndex >= totalDelimiters && hasNext) {
                char appendChar = schema.getDelimiterAfter(activeArgIndex);
                editBox.setValue(fullSoFar + appendChar);
            } else {
                editBox.setValue(fullSoFar);
            }
            
            editBox.setCursorPosition(editBox.getValue().length());
            currentSuggestions.clear();
        }

        public void render(GuiGraphics gfx, int mX, int mY, float partialTick) {
            if (!isActive()) return;
            int x = editBox.getX();
            int y = editBox.getY() + editBox.getHeight();
            int w = editBox.getWidth();
            int visibleCount = Math.min(currentSuggestions.size(), maxVisible);
            int h = visibleCount * 14 + 4;
            
            gfx.fill(x, y, x + w, y + h, 0xFA000000); 
            gfx.renderOutline(x, y, w, h, 0xFF555555);
            
            boolean mouseMoved = (mX != lastMouseX || mY != lastMouseY);
            lastMouseX = mX;
            lastMouseY = mY;
            
            for (int i = 0; i < visibleCount; i++) {
                int actualIndex = i + scrollOffset;
                if(actualIndex >= currentSuggestions.size()) break;
                
                SchemaInfo.Suggestion sug = currentSuggestions.get(actualIndex);
                int rowY = y + 2 + i * 14;
                
                boolean isHovered = mX >= x && mX <= x + w && mY >= rowY && mY < rowY + 14;
                
                if (isHovered && mouseMoved) {
                    selectedIndex = actualIndex;
                }
                
                if (actualIndex == selectedIndex) {
                    gfx.fill(x + 1, rowY, x + w - (currentSuggestions.size() > maxVisible ? 6 : 1), rowY + 14, 0xAA0055AA);
                }
                
                int drawX = x + 4;
                if (sug.colorPreview != null) {
                    gfx.fill(drawX, rowY + 3, drawX + 8, rowY + 11, 0xFF000000 | sug.colorPreview);
                    gfx.renderOutline(drawX, rowY + 3, 8, 8, 0xFF888888);
                    drawX += 12;
                }
                
                gfx.drawString(font, sug.value, drawX, rowY + 3, (actualIndex == selectedIndex) ? 0xFFFFFF : 0xAAAAAA, false);
                if (!sug.description.isEmpty()) {
                    gfx.drawString(font, "- " + sug.description, drawX + font.width(sug.value) + 5, rowY + 3, 0xFF888888, false);
                }
            }
            
            if (currentSuggestions.size() > maxVisible) {
                int scrollBarH = (int) (((float)maxVisible / currentSuggestions.size()) * h);
                int scrollBarY = y + (int) (((float)scrollOffset / (currentSuggestions.size() - maxVisible)) * (h - scrollBarH));
                gfx.fill(x + w - 5, y + 1, x + w - 1, y + h - 1, 0xFF222222);
                gfx.fill(x + w - 5, scrollBarY + 1, x + w - 1, scrollBarY + scrollBarH - 1, 0xFF888888);
            }
        }

        @Override public void setFocused(boolean p_265728_) {}
        @Override public boolean isFocused() { return false; }
        @Override public NarrationPriority narrationPriority() { return NarrationPriority.NONE; }
        @Override public void updateNarration(NarrationElementOutput output) {}
    }

    public static class HelpTextWidget extends ContainerObjectSelectionList<GuideLineEntry> {
        public HelpTextWidget(Minecraft mc, int w, int h, int t, int b, int ih) { super(mc, w, h, t, b, ih); }
        @Override public int addEntry(GuideLineEntry entry) { return super.addEntry(entry); } 
        @Override public int getRowWidth() { return this.width - 80; }
        @Override protected int getScrollbarPosition() { return this.width - 20; }
    }

    public static class GuideLineEntry extends ContainerObjectSelectionList.Entry<GuideLineEntry> {
        protected net.minecraft.util.FormattedCharSequence text;
        public GuideLineEntry(net.minecraft.util.FormattedCharSequence text) { this.text = text; }
        public GuideLineEntry() {}
        @Override public void render(GuiGraphics gfx, int idx, int top, int left, int w, int h, int mX, int mY, boolean hover, float pt) {
            if(text != null) gfx.drawString(Minecraft.getInstance().font, text, left, top + 2, 0xDDDDDD, false);
        }
        @Override public List<? extends GuiEventListener> children() { return Collections.emptyList(); }
        @Override public List<? extends NarratableEntry> narratables() { return Collections.emptyList(); }
    }
    
    public static class TableRowEntry extends GuideLineEntry {
        private final net.minecraft.util.FormattedCharSequence[] columns;
        private final List<Float> widthRatios;
        private final boolean isHeader;
        private final SchemaInfo schema;

        public TableRowEntry(net.minecraft.util.FormattedCharSequence[] columns, List<Float> widthRatios, boolean isHeader, SchemaInfo schema) { 
            this.columns = columns; 
            this.widthRatios = widthRatios; 
            this.isHeader = isHeader;
            this.schema = schema;
        }
        @Override public void render(GuiGraphics gfx, int idx, int top, int left, int w, int h, int mX, int mY, boolean hover, float pt) {
            int currentX = left + 5;
            for (int i = 0; i < columns.length; i++) {
                if (i >= widthRatios.size()) break;
                if(columns[i] != null) gfx.drawString(Minecraft.getInstance().font, columns[i], currentX, top + 2, isHeader ? 0xFFFFAA : 0xAAAAAA, false);
                currentX += (int) (w * widthRatios.get(i));
            }
        }
    }

    public static class TableSeparatorEntry extends GuideLineEntry {
        private final List<Float> widthRatios;
        public TableSeparatorEntry(List<Float> widthRatios) { this.widthRatios = widthRatios; }
        @Override public void render(GuiGraphics gfx, int idx, int top, int left, int w, int h, int mX, int mY, boolean hover, float pt) {
            gfx.fill(left, top + h/2, left + w, top + h/2 + 1, 0xFF555555);
        }
    }
}