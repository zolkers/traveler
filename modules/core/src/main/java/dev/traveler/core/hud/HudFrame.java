package dev.traveler.core.hud;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record HudFrame(List<HudBox> boxes, List<HudText> texts) {
    public HudFrame {
        boxes = List.copyOf(Objects.requireNonNull(boxes, "boxes"));
        texts = List.copyOf(Objects.requireNonNull(texts, "texts"));
    }

    public Optional<HudBox> box(String id) {
        return boxes.stream().filter(box -> box.id().equals(id)).findFirst();
    }

    public Optional<HudText> text(String id) {
        return texts.stream().filter(text -> text.id().equals(id)).findFirst();
    }
}
