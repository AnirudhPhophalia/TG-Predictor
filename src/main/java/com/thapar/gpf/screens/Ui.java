package com.thapar.gpf.screens;

import com.thapar.gpf.App;
import com.thapar.gpf.AppException;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

/** Small building blocks shared by every screen. Colours and fonts live in app.css. */
public final class Ui {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private Ui() {
    }

    // ---------- text ----------

    public static Label text(String value, String... styleClasses) {
        Label label = new Label(value);
        label.setWrapText(true);
        label.getStyleClass().addAll(styleClasses);
        return label;
    }

    public static Label eyebrow(String value) {
        return text(value, "eyebrow");
    }

    public static Label h1(String value) {
        return text(value, "h1");
    }

    public static Label h2(String value) {
        return text(value, "h2");
    }

    public static Label h3(String value) {
        return text(value, "h3");
    }

    public static Label muted(String value) {
        return text(value, "muted");
    }

    public static Label notice(String value) {
        Label label = text(value, "notice");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    public static Label badge(String value) {
        return text(value, "badge");
    }

    // ---------- layout ----------

    public static VBox page(Node... children) {
        VBox page = new VBox(16, children);
        page.getStyleClass().add("page");
        return page;
    }

    /** Eyebrow, big title and a muted subtitle. Any of them may be null. */
    public static VBox heading(String eyebrow, String title, String subtitle) {
        VBox box = new VBox(4);
        if (eyebrow != null) box.getChildren().add(eyebrow(eyebrow));
        if (title != null) box.getChildren().add(h1(title));
        if (subtitle != null) box.getChildren().add(muted(subtitle));
        return box;
    }

    public static VBox card(Node... children) {
        VBox card = new VBox(10, children);
        card.getStyleClass().add("card");
        return card;
    }

    public static HBox row(Node... children) {
        HBox row = new HBox(10, children);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    public static FlowPane tiles(List<? extends Node> children) {
        FlowPane tiles = new FlowPane(14, 14);
        tiles.getChildren().addAll(children);
        return tiles;
    }

    public static Region grow() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    public static VBox metric(String label, String value, String caption) {
        VBox box = card(eyebrow(label), text(value, "number"));
        if (caption != null) box.getChildren().add(text(caption, "small", "muted"));
        box.getStyleClass().add("metric");
        return box;
    }

    // ---------- buttons ----------

    /** A button whose action errors are shown as a red banner instead of crashing the app. */
    public static Button button(String text, Runnable action) {
        Button button = new Button(text);
        button.setOnAction(e -> App.act(action));
        return button;
    }

    public static Button secondary(String text, Runnable action) {
        Button button = button(text, action);
        button.getStyleClass().add("secondary");
        return button;
    }

    public static Button danger(String text, Runnable action) {
        Button button = button(text, action);
        button.getStyleClass().add("danger");
        return button;
    }

    public static Hyperlink link(String text, Runnable action) {
        Hyperlink link = new Hyperlink(text);
        link.setOnAction(e -> App.act(action));
        return link;
    }

    /** Makes a whole row or card clickable with the mouse, and with Enter or Space after tabbing to it. */
    public static <T extends Region> T clickable(T content, String styleClass, Runnable action) {
        content.getStyleClass().addAll("row-button", styleClass);
        content.setFocusTraversable(true);
        content.setOnMouseClicked(e -> App.act(action));
        content.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) App.act(action);
        });
        return content;
    }

    /** Label/value pairs, two pairs per row, like a small statistics sheet. */
    public static GridPane facts(String... labelsAndValues) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("facts");
        for (int i = 0; i < labelsAndValues.length; i += 2) {
            int row = i / 4, column = (i / 2) % 2 * 2;
            grid.add(text(labelsAndValues[i], "fact-label"), column, row);
            grid.add(text(labelsAndValues[i + 1], "fact-value"), column + 1, row);
        }
        return grid;
    }

    // ---------- forms ----------

    public static TextField field(Object value, String prompt) {
        TextField field = new TextField(value == null ? "" : clean(value));
        field.setPromptText(prompt);
        return field;
    }

    public static VBox labeled(String label, Node control) {
        return new VBox(4, text(label, "field-label"), control);
    }

    /** Blank means "not given" (null). Anything else must be a number. */
    public static Double number(TextField field, String name) {
        String value = field.getText().trim();
        if (value.isEmpty()) return null;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new AppException(name + " must be a valid number");
        }
    }

    public static Integer whole(TextField field, String name) {
        String value = field.getText().trim();
        if (value.isEmpty()) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new AppException(name + " must be a whole number");
        }
    }

    /** "All semesters" (when allowed) followed by Semester 1..8. */
    public static ComboBox<String> semesterPicker(Integer selected, boolean allowAll) {
        ComboBox<String> box = new ComboBox<>();
        if (allowAll) box.getItems().add("All semesters");
        IntStream.rangeClosed(1, 8).forEach(n -> box.getItems().add("Semester " + n));
        box.getSelectionModel().select(selected == null ? (allowAll ? "All semesters" : "Semester 1") : "Semester " + selected);
        return box;
    }

    public static Integer semesterOf(ComboBox<String> box) {
        String value = box.getValue();
        return value == null || !value.startsWith("Semester ") ? null : Integer.parseInt(value.substring(9));
    }

    // ---------- tables and pagination ----------

    /** One table column: a title and how to get the cell from a row. A cell may be text or any control. */
    public record Col<T>(String title, Function<T, Object> value) {
    }

    @SafeVarargs
    public static <T> TableView<T> table(List<T> rows, Col<T>... columns) {
        TableView<T> table = new TableView<>();
        for (Col<T> col : columns) {
            TableColumn<T, Object> column = new TableColumn<>(col.title());
            column.setSortable(false);
            column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(col.value().apply(cell.getValue())));
            column.setCellFactory(c -> new TableCell<>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(null);
                    setGraphic(null);
                    setTooltip(null);
                    if (empty || item == null) return;
                    if (item instanceof Node node) {
                        setGraphic(node);
                        return;
                    }
                    setText(item.toString());
                    if (item.toString().length() > 24) setTooltip(new Tooltip(item.toString()));   // full text on hover
                }
            });
            table.getColumns().add(column);
        }
        table.getItems().setAll(rows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setFixedCellSize(42);
        table.setPrefHeight(42 * Math.max(1, rows.size()) + 34);
        table.setMinHeight(table.getPrefHeight());
        table.setPlaceholder(muted("Nothing here yet."));
        return table;
    }

    private static final Map<String, Integer> lastPage = new HashMap<>();

    /**
     * Shows a long list a page at a time with JavaFX's own Pagination control.
     * The key remembers the page, so approving a request on page 3 keeps you on page 3.
     */
    // ponytail: slices a list already in memory; switch to SQL OFFSET/FETCH if a table grows to tens of thousands of rows
    public static <T> Node paged(String key, List<T> items, int perPage, Function<List<T>, Node> render) {
        if (items.size() <= perPage) return render.apply(items);
        int pages = (items.size() + perPage - 1) / perPage;
        Pagination pagination = new Pagination(pages, Math.min(lastPage.getOrDefault(key, 0), pages - 1));
        pagination.setMaxPageIndicatorCount(Math.min(pages, 7));
        pagination.setPageFactory(page -> {
            lastPage.put(key, page);
            int from = page * perPage;
            return render.apply(items.subList(from, Math.min(items.size(), from + perPage)));
        });
        Label total = text(items.size() + " items, " + perPage + " per page", "small", "muted");
        return new VBox(4, pagination, total);
    }

    // ---------- charts ----------

    /** Response counts in the ten score ranges 0-10 ... 90-100. */
    public static BarChart<String, Number> distribution(int[] buckets) {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        x.setLabel("Score range");
        y.setLabel("Responses");
        y.setMinorTickVisible(false);
        BarChart<String, Number> chart = new BarChart<>(x, y);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int i = 0; i < buckets.length; i++) {
            series.getData().add(new XYChart.Data<>(i * 10 + "-" + (i * 10 + 10), buckets[i]));
        }
        chart.getData().add(series);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(260);
        chart.setCategoryGap(6);
        return chart;
    }

    // ---------- formatting ----------

    public static String fmt(double value, int decimals) {
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    /** 58.0 shows as 58, 58.5 as 58.5. */
    public static String clean(Object value) {
        if (value instanceof Double d) return d == Math.rint(d) ? String.valueOf(d.longValue()) : String.valueOf(d);
        return String.valueOf(value);
    }

    public static String orDash(Object value) {
        return value == null ? "-" : clean(value);
    }

    public static String when(LocalDateTime time) {
        return time == null ? "-" : DATE_TIME.format(time);
    }

    public static String day(LocalDateTime time) {
        return time == null ? "-" : DATE.format(time);
    }
}
