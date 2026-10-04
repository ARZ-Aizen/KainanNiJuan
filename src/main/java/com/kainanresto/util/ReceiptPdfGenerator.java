package com.kainanresto.util;

import com.kainanresto.model.order.OrderReceipt;
import com.kainanresto.model.transac.PaymentResult;
import com.kainanresto.model.transac.ReceiptLine;
import com.kainanresto.model.transac.ReceiptTotals;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ReceiptPdfGenerator {

    private static final float PAGE_W = 226.77f;            // 80 mm
    private static final float MARGIN = 14f;
    private static final float CONTENT_W = PAGE_W - MARGIN * 2;
    private static final float QTY_W = 28f, AMT_W = 64f, COL_GAP = 6f;
    private static final float LINE_H = 12f;

    private static final PDFont REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont ITALIC = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy  h:mm a", Locale.ENGLISH);

    private interface Draw { void run(PDPageContentStream cs, float top) throws IOException; }
    private record Element(float height, Draw draw) {}

    private ReceiptPdfGenerator() {}

    public static File generate(OrderReceipt r, PaymentResult pay) throws IOException {
        List<Element> elements = build(r, pay);
        float contentH = 0;
        for (Element e : elements) contentH += e.height();
        float pageH = Math.max(200f, contentH + MARGIN * 2);

        Path dir = Path.of(System.getProperty("user.home"), "Documents", "KainanNiJuan", "Receipts");
        Files.createDirectories(dir);
        String safeNo = String.valueOf(r.orderNumber()).replaceAll("[^A-Za-z0-9_-]", "_");
        File out = dir.resolve("receipt-" + safeNo + ".pdf").toFile();

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(PAGE_W, pageH));
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = pageH - MARGIN;
                for (Element e : elements) {
                    e.draw().run(cs, y);
                    y -= e.height();
                }
            }
            doc.save(out);
        }
        return out;
    }

    /* ============================== LAYOUT ============================== */

    private static List<Element> build(OrderReceipt r, PaymentResult pay) throws IOException {
        ReceiptTotals t = r.totals();
        List<Element> els = new ArrayList<>();

        els.add(center("KAINAN NI JUAN", BOLD, 14f, 20f));
        els.add(center("Order Receipt", REGULAR, 9f, 14f));
        els.add(dash());

        String type = (r.orderType() == null || r.orderType().isBlank()) ? "-" : r.orderType();
        if (r.orderTypeDetail() != null && !r.orderTypeDetail().isBlank()) type += " - " + r.orderTypeDetail().trim();
        els.add(kv("Order No.", r.orderNumber(), REGULAR, 8.5f));
        els.add(kv("Date", r.createdAt() == null ? "-" : r.createdAt().format(DATE_FMT), REGULAR, 8.5f));
        els.add(kv("Cashier", r.cashierName(), REGULAR, 8.5f));
        els.add(kv("Order type", type, REGULAR, 8.5f));
        els.add(dash());

        els.add(itemHeader());
        for (ReceiptLine l : r.lines()) els.add(itemRow(l));
        els.add(dash());

        els.add(kv("Subtotal", money(t.subtotal()), REGULAR, 9f));
        BigDecimal discount = t.subtotal().add(t.serviceCharge()).add(t.vat()).subtract(t.total());
        if (discount.signum() > 0) {
            String label = r.discountName() == null ? "Discount" : "Discount - " + r.discountName();
            els.add(kv(label, "-" + money(discount), REGULAR, 9f));
        }
        els.add(kv("Service charge" + rateSuffix(t.serviceChargeRate()), money(t.serviceCharge()), REGULAR, 9f));
        els.add(kv("VAT" + rateSuffix(t.vatRate()), money(t.vat()), REGULAR, 9f));
        els.add(solid());
        els.add(kv("TOTAL", money(t.total()), BOLD, 12f, 20f));
        els.add(dash());

        if (pay != null) {
            els.add(kv("Cash", money(pay.cashTendered()), REGULAR, 9f));
            els.add(kv("Change", money(pay.change()), REGULAR, 9f));
            els.add(dash());
        }
        els.add(center("Thank you! Please come again.", ITALIC, 8.5f, 16f));
        return els;
    }

    private static Element center(String text, PDFont font, float size, float height) {
        String s = safe(text, font);
        return new Element(height, (cs, top) -> {
            float x = (PAGE_W - width(s, font, size)) / 2f;
            text(cs, font, size, x, top - size, s);
        });
    }

    private static Element kv(String key, String value, PDFont font, float size) {
        return kv(key, value, font, size, LINE_H + 2f);
    }

    private static Element kv(String key, String value, PDFont font, float size, float height) {
        String k = safe(key, font), v = safe(value == null ? "-" : value, font);
        return new Element(height, (cs, top) -> {
            text(cs, font, size, MARGIN, top - size, k);
            text(cs, font, size, MARGIN + CONTENT_W - width(v, font, size), top - size, v);
        });
    }

    private static Element itemHeader() {
        return new Element(LINE_H + 4f, (cs, top) -> {
            float size = 8.5f, base = top - size;
            float qtyX = MARGIN + CONTENT_W - AMT_W - COL_GAP - QTY_W;
            text(cs, BOLD, size, MARGIN, base, "Item");
            text(cs, BOLD, size, qtyX + (QTY_W - width("Qty", BOLD, size)) / 2f, base, "Qty");
            text(cs, BOLD, size, MARGIN + CONTENT_W - width("Amount", BOLD, size), base, "Amount");
        });
    }

    private static Element itemRow(ReceiptLine l) throws IOException {
        float size = 9f;
        float nameW = CONTENT_W - QTY_W - AMT_W - COL_GAP * 2;
        List<String> nameLines = wrap(safe(l.name() == null ? "-" : l.name(), REGULAR), REGULAR, size, nameW);
        String unit = safe(money(l.unitPrice()), REGULAR);
        String qty = String.valueOf(l.quantity());
        String amt = safe(money(l.lineTotal()), REGULAR);
        float height = nameLines.size() * LINE_H + 10f + 6f;

        return new Element(height, (cs, top) -> {
            float base = top - size;
            for (String line : nameLines) {
                text(cs, REGULAR, size, MARGIN, base, line);
                base -= LINE_H;
            }
            cs.setNonStrokingColor(0.45f);
            text(cs, REGULAR, 7.5f, MARGIN, base, unit);
            cs.setNonStrokingColor(0f);

            float firstBase = top - size;
            float qtyX = MARGIN + CONTENT_W - AMT_W - COL_GAP - QTY_W;
            text(cs, REGULAR, size, qtyX + (QTY_W - width(qty, REGULAR, size)) / 2f, firstBase, qty);
            text(cs, REGULAR, size, MARGIN + CONTENT_W - width(amt, REGULAR, size), firstBase, amt);
        });
    }

    private static Element dash() {
        return new Element(12f, (cs, top) -> {
            float y = top - 6f;
            cs.setLineDashPattern(new float[]{2f, 2f}, 0);
            cs.setLineWidth(0.5f);
            cs.setStrokingColor(0.5f);
            cs.moveTo(MARGIN, y);
            cs.lineTo(MARGIN + CONTENT_W, y);
            cs.stroke();
            cs.setLineDashPattern(new float[0], 0);
            cs.setStrokingColor(0f);
        });
    }

    private static Element solid() {
        return new Element(12f, (cs, top) -> {
            float y = top - 6f;
            cs.setLineWidth(1f);
            cs.moveTo(MARGIN, y);
            cs.lineTo(MARGIN + CONTENT_W, y);
            cs.stroke();
        });
    }

    /* ============================== HELPERS ============================== */

    private static void text(PDPageContentStream cs, PDFont font, float size, float x, float y, String s)
            throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(s);
        cs.endText();
    }

    private static float width(String s, PDFont font, float size) {
        try {
            return font.getStringWidth(s) / 1000f * size;
        } catch (IOException | IllegalArgumentException e) {
            return 0f;
        }
    }

    private static String safe(String s, PDFont font) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            try {
                font.encode(String.valueOf(c));
                sb.append(c);
            } catch (Exception e) {
                sb.append('?');
            }
        }
        return sb.toString();
    }

    private static List<String> wrap(String text, PDFont font, float size, float maxW) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = cur.length() == 0 ? word : cur + " " + word;
            if (width(candidate, font, size) <= maxW) {
                cur = new StringBuilder(candidate);
                continue;
            }
            if (cur.length() > 0) {
                lines.add(cur.toString());
                cur.setLength(0);
            }
            while (word.length() > 1 && width(word, font, size) > maxW) {
                int n = word.length();
                while (n > 1 && width(word.substring(0, n), font, size) > maxW) n--;
                lines.add(word.substring(0, n));
                word = word.substring(n);
            }
            cur.append(word);
        }
        if (cur.length() > 0 || lines.isEmpty()) lines.add(cur.toString());
        return lines;
    }

    private static String money(BigDecimal amount) {
        return amount == null ? "-" : "PHP " + String.format(Locale.ENGLISH, "%,.2f", amount);
    }

    private static String rateSuffix(BigDecimal rate) {
        return rate == null ? "" : " (" + rate.stripTrailingZeros().toPlainString() + "%)";
    }
}