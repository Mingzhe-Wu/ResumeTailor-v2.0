import html2canvas from "html2canvas";
import { jsPDF } from "jspdf";

export async function exportResumeElementToPdf(element, filename) {
  await document.fonts?.ready;
  // Export the exact rendered resume DOM, but toggle export-only CSS to hide
  // editor affordances such as placeholders and add-bullet buttons.
  element.classList.add("pdf-exporting");
  element.classList.add("exporting-pdf");

  try {
    const canvas = await html2canvas(element, {
      scale: 2,
      useCORS: true,
      backgroundColor: "#ffffff",
      scrollX: -window.scrollX,
      scrollY: -window.scrollY,
      logging: false,
    });

    const imageData = canvas.toDataURL("image/png");
    const pdf = new jsPDF({
      orientation: "portrait",
      unit: "px",
      format: [canvas.width, canvas.height],
    });

    pdf.addImage(
      imageData,
      "PNG",
      0,
      0,
      pdf.internal.pageSize.getWidth(),
      pdf.internal.pageSize.getHeight()
    );
    pdf.save(filename);
  } finally {
    element.classList.remove("pdf-exporting");
    element.classList.remove("exporting-pdf");
  }
}
