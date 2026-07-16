import html2canvas from "html2canvas";
import { jsPDF } from "jspdf";

const PDF_CAPTURE_SCALE = 1.5;
const PDF_TARGET_BYTES = 950 * 1024;
const PDF_JPEG_QUALITIES = [0.82, 0.74, 0.66, 0.58];
const PRINT_ROOT_CLASS = "resume-printing";
const PRINT_TARGET_CLASS = "resume-print-target";
const PRINT_PAGE_STYLE_ID = "resume-print-page-size";
const CSS_PIXELS_PER_INCH = 96;
const PRINT_STYLE_PROPERTIES = [
  ["--resume-print-width", "width"],
  ["--resume-print-height", "height"],
  ["--resume-print-padding-top", "paddingTop"],
  ["--resume-print-padding-right", "paddingRight"],
  ["--resume-print-padding-bottom", "paddingBottom"],
  ["--resume-print-padding-left", "paddingLeft"],
];

export function isResumeElementOutOfBoundary(element) {
  return element.scrollHeight > element.clientHeight + 1;
}

export async function printResumeElement(element, filename) {
  if (!element) throw new Error("Resume element is required for printing.");

  await document.fonts?.ready;

  const root = document.documentElement;
  const previousTitle = document.title;
  const pageStyle = document.createElement("style");
  const bounds = element.getBoundingClientRect();
  const computedStyle = window.getComputedStyle(element);
  const printWidth = bounds.width || element.offsetWidth;
  const printHeight = bounds.height || element.offsetHeight;
  let cleanedUp = false;

  const cleanup = () => {
    if (cleanedUp) return;
    cleanedUp = true;
    root.classList.remove(PRINT_ROOT_CLASS);
    element.classList.remove(PRINT_TARGET_CLASS, "pdf-exporting", "exporting-pdf");
    PRINT_STYLE_PROPERTIES.forEach(([property]) => element.style.removeProperty(property));
    pageStyle.remove();
    document.title = previousTitle;
    window.removeEventListener("afterprint", cleanup);
  };

  root.classList.add(PRINT_ROOT_CLASS);
  element.classList.add(PRINT_TARGET_CLASS, "pdf-exporting", "exporting-pdf");
  PRINT_STYLE_PROPERTIES.forEach(([property, computedProperty]) => {
    const value = computedProperty === "width"
      ? `${printWidth}px`
      : computedProperty === "height"
        ? `${printHeight}px`
        : computedStyle[computedProperty];
    element.style.setProperty(property, value);
  });
  pageStyle.id = PRINT_PAGE_STYLE_ID;
  pageStyle.textContent = `@page { size: ${printWidth / CSS_PIXELS_PER_INCH}in ${printHeight / CSS_PIXELS_PER_INCH}in; margin: 0; }`;
  document.head.appendChild(pageStyle);
  document.title = String(filename || "Resume.pdf").replace(/\.pdf$/i, "");
  window.addEventListener("afterprint", cleanup, { once: true });

  try {
    window.print();
  } catch (error) {
    cleanup();
    throw error;
  }

  // Some browsers do not dispatch afterprint when the dialog is cancelled.
  window.setTimeout(cleanup, 1000);
}

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
