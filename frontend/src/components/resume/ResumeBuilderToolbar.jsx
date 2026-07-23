import {
  getResumeSectionKey,
  getResumeSectionTitle,
  sortResumeSections,
} from "./resumeUtils.js";

export default function ResumeBuilderToolbar({ resume, onSummaryToggle, onSectionToggle }) {
  if (!resume || typeof resume !== "object") return null;

  const sections = Array.isArray(resume.sections)
    ? sortResumeSections(resume.sections)
    : [];

  return (
    <div className="resume-builder-toolbar">
      <label>
        <input
          type="checkbox"
          checked={resume.summary?.visible !== false}
          onChange={(e) => onSummaryToggle(e.target.checked)}
        />
        Summary
      </label>

      {sections.map((section) => {
        const type = String(section.type || "").toLowerCase();
        const sectionKey = getResumeSectionKey(section);

        return (
          <label key={sectionKey}>
            <input
              type="checkbox"
              checked={section.visible !== false}
              onChange={(e) => onSectionToggle(sectionKey, e.target.checked)}
            />
            {section.title || getResumeSectionTitle(type)}
          </label>
        );
      })}
    </div>
  );
}
