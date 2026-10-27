package com.myhourly.jewellery.billing.service;

import com.myhourly.jewellery.billing.dto.InvoiceItemResponse;
import com.myhourly.jewellery.billing.dto.InvoiceResponse;
import org.springframework.stereotype.Component;

@Component
public class InvoiceHtmlRenderer {

    public String render(InvoiceResponse invoice) {
        StringBuilder rows = new StringBuilder();
        for (InvoiceItemResponse item : invoice.items()) {
            rows.append("<tr>")
                .append("<td>").append(item.productId()).append("</td>")
                .append("<td>").append(item.netWeight()).append("</td>")
                .append("<td>").append(item.ratePerGram()).append("</td>")
                .append("<td>").append(item.metalValue()).append("</td>")
                .append("<td>").append(item.makingCharge()).append("</td>")
                .append("<td>").append(item.lineTotal()).append("</td>")
                .append("</tr>");
        }

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">")
            .append("<title>Invoice ").append(invoice.invoiceNumber()).append("</title>")
            .append("<style>")
            .append("body{font-family:Arial,sans-serif;margin:40px;}")
            .append("table{border-collapse:collapse;width:100%;margin-top:20px;}")
            .append("th,td{border:1px solid #999;padding:8px;text-align:right;}")
            .append("th{background:#eee;}")
            .append(".totals{margin-top:20px;width:320px;margin-left:auto;}")
            .append(".totals div{display:flex;justify-content:space-between;padding:4px 0;}")
            .append(".grand{font-weight:bold;border-top:2px solid #000;}")
            .append("</style></head><body>")
            .append("<h1>Jewellery Shop - Invoice</h1>")
            .append("<p><strong>Invoice No:</strong> ").append(invoice.invoiceNumber()).append("<br>")
            .append("<strong>Date:</strong> ").append(invoice.createdAt()).append("<br>")
            .append("<strong>Customer ID:</strong> ").append(invoice.customerId()).append("<br>")
            .append("<strong>Order ID:</strong> ").append(invoice.orderId()).append("<br>")
            .append("<strong>Status:</strong> ").append(invoice.status()).append("</p>")
            .append("<table><tr><th>Product</th><th>Net weight (g)</th><th>Rate/g</th>")
            .append("<th>Metal value</th><th>Making charge</th><th>Line total</th></tr>")
            .append(rows)
            .append("</table><div class=\"totals\">")
            .append("<div><span>Subtotal</span><span>").append(invoice.subtotal()).append("</span></div>")
            .append("<div><span>Discount</span><span>").append(invoice.discount()).append("</span></div>")
            .append("<div><span>Taxable amount</span><span>").append(invoice.taxableAmount()).append("</span></div>")
            .append("<div><span>Tax</span><span>").append(invoice.tax()).append("</span></div>")
            .append("<div class=\"grand\"><span>Grand total</span><span>").append(invoice.grandTotal()).append("</span></div>")
            .append("</div></body></html>");
        return html.toString();
    }
}