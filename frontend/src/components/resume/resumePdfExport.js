import html2canvas from "html2canvas";
import { jsPDF } from "jspdf";

const PDF_CAPTURE_SCALE = 1.5;
const PDF_TARGET_BYTES = 950 * 1024;
const PDF_JPEG_QUALITIES = [0.82, 0.74, 0.66, 0.58];

export async function exportResumeElementToPdf(element, filename) {
  await document.fonts?.ready;
  // Export the exact rendered resume DOM, but toggle export-only CSS to hide
  // editor affordances such as placeholders and add-bullet buttons.
  element.classList.add("pdf-exporting");
  element.classList.add("exporting-pdf");

  try {
    const canvas = await html2canvas(element, {
      scale: PDF_CAPTURE_SCALE,
      useCORS: true,
      backgroundColor: "#ffffff",
      scrollX: -window.scrollX,
      scrollY: -window.scrollY,
      logging: false,
    });

    const pdf = buildCompressedImagePdf(canvas);
    pdf.save(filename);
  } finally {
    element.classList.remove("pdf-exporting");
    element.classList.remove("exporting-pdf");
  }
}

function buildCompressedImagePdf(canvas) {
  let fallbackPdf = null;

  for (const quality of PDF_JPEG_QUALITIES) {
    const imageData = canvas.toDataURL("image/jpeg", quality);
    const pdf = createImagePdf(canvas, imageData);
    const byteLength = pdf.output("arraybuffer").byteLength;

    fallbackPdf = pdf;
    if (byteLength <= PDF_TARGET_BYTES) {
      return pdf;
    }
  }

  return fallbackPdf;
}

function createImagePdf(canvas, imageData) {
  const pdf = new jsPDF({
    orientation: "portrait",
    unit: "px",
    format: [canvas.width, canvas.height],
    compress: true,
  });

  pdf.addImage(
    imageData,
    "JPEG",
    0,
    0,
    pdf.internal.pageSize.getWidth(),
    pdf.internal.pageSize.getHeight(),
    undefined,
    "MEDIUM"
  );

  return pdf;
}
