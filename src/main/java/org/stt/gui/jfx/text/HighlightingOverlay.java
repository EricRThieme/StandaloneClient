package org.stt.gui.jfx.text;

import com.sun.javafx.scene.control.skin.TextAreaSkin;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TextArea;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by dante on 06.12.14.
 */
public class HighlightingOverlay {
    private static final Logger LOG = Logger.getLogger(HighlightingOverlay.class
            .getName());

    private Region region;
    private ObservableList<Node> children;
    private TextArea target;
    private TextAreaSkin textAreaSkin;
    private List<Highlight> highlights = new ArrayList<>();

    public HighlightingOverlay(TextArea target) {
        this.target = checkNotNull(target);
        updateInternalAccess();
    }

    @SuppressWarnings("unchecked")
    private boolean updateInternalAccess() {
        textAreaSkin = (TextAreaSkin) target.getSkin();
        if (textAreaSkin == null) {
            return false;
        }
        try {
            Field contentView = TextAreaSkin.class.getDeclaredField("contentView");
            contentView.setAccessible(true);
            region = (Region) contentView.get(target.getSkin());
            contentView.setAccessible(false);

            Method getChildren = Parent.class.getDeclaredMethod("getChildren");
            getChildren.setAccessible(true);
            children = (ObservableList<Node>) getChildren.invoke(region);
            getChildren.setAccessible(false);
            return true;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to apply text highlighting", e);
            return false;
        }
    }

    public void addHighlight(Highlight highlight) {
        checkNotNull(highlight);
        if (region == null && !updateInternalAccess()) {
            return;
        }

        // avoid adding duplicate highlights for the exact same range and color
        for (Highlight h : highlights) {
            if (h.matchesRangeAndColor(highlight)) {
                return;
            }
        }

        // layout rectangles before adding so positions are computed
        highlight.layoutUsing(target, region, textAreaSkin);

        // Merge adjacent/overlapping highlights of the same color
        for (Highlight h : highlights) {
            if (h.color != null && h.color.equals(highlight.color) && (h.end + 1 >= highlight.start && h.start - 1 <= highlight.end)) {
                // merge ranges
                children.remove(h.rectangle);
                h.start = Math.min(h.start, highlight.start);
                h.end = Math.max(h.end, highlight.end);
                h.layoutUsing(target, region, textAreaSkin);
                children.add(h.rectangle);
                return;
            }
        }

        // if any existing highlight overlaps this range, skip adding to avoid stacked opacity
        for (Highlight h : highlights) {
            if (h.start <= highlight.end && h.end >= highlight.start) {
                return;
            }
        }

        highlight.layoutUsing(target, region, textAreaSkin);
        children.add(highlight.rectangle);
        highlights.add(highlight);
    }

    public void clearHighlights() {
        if (children == null) {
            return;
        }
        for (Highlight h: highlights) {
            children.remove(h.rectangle);
        }
        highlights.clear();
    }

    public static class Highlight {

        protected Rectangle rectangle;
        private int start;
        private int end;
        private Color color;

        public Highlight(int from, int to, Color color) {
            this.start = from;
            this.end = to;
            this.color = color;
            Rectangle rec = new Rectangle();
            rec.setDisable(true);
            rec.setMouseTransparent(true);
            rec.setBlendMode(null);
            rec.setOpacity(0.35);
            rec.setFill(color);
            this.rectangle = rec;
        }

        public boolean matchesRangeAndColor(Highlight other) {
            if (other == null) return false;
            if (this.start != other.start) return false;
            if (this.end != other.end) return false;
            if (this.color == null && other.color == null) return true;
            if (this.color == null || other.color == null) return false;
            return this.color.equals(other.color);
        }

        public boolean containsIndex(int pos) {
            return pos >= this.start && pos <= this.end;
        }

        protected void layoutUsing(TextArea target, Region within, TextAreaSkin skin) {
            // compute bounding rectangle from first to last character (use precise doubles, avoid per-char rounding)
            Rectangle2D firstBounds = skin.getCharacterBounds(start);
            Rectangle2D lastBounds = skin.getCharacterBounds(end);

            Point2D pFirst = target.localToScene(firstBounds.getMinX(), firstBounds.getMinY());
            pFirst = within.sceneToLocal(pFirst);
            Point2D pLastRight = target.localToScene(lastBounds.getMinX() + lastBounds.getWidth(), lastBounds.getMinY());
            pLastRight = within.sceneToLocal(pLastRight);

            double x = pFirst.getX();
            double y = pFirst.getY();
            double w = Math.max(1.0, pLastRight.getX() - pFirst.getX());
            double h = Math.max(1.0, Math.max(firstBounds.getHeight(), lastBounds.getHeight()));

            rectangle.setX(x);
            rectangle.setY(y);
            rectangle.setWidth(w);
            rectangle.setHeight(h);
        }
    }
}
