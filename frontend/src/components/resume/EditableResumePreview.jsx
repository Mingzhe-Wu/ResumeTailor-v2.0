import EditableResumeSection from "./EditableResumeSection.jsx";
import EditableText from "./EditableText.jsx";
import { getResumeSectionKey } from "./resumeUtils.js";

export default function EditableResumePreview({ resume, onChange, resumeRef, outOfBoundary = false }) {
  if (!resume || typeof resume !== "object") {
    return (
      <div className="resume-empty-state">
        <h3>Resume preview unavailable</h3>
        <p>The generated resume content is not in the expected structured format.</p>
      </div>
    );
  }

  const contact = resume.contact || {};
  const sections = Array.isArray(resume.sections)
    ? [...resume.sections]
        .filter((section) => section.visible !== false)
        .sort((a, b) => (a.order ?? 0) - (b.order ?? 0))
    : [];

  const contactFields = [
    ["location", contact.location],
    ["email", contact.email],
    ["phone", contact.phone],
    ["linkedin", contact.linkedin],
    ["github", contact.github],
  ];
  let visibleContactCount = 0;

  const updateContact = (field, value) => {
    onChange({
      ...resume,
      contact: {
        ...(resume.contact || {}),
        [field]: value,
      },
    });
  };

  const updateSummary = (value) => {
    onChange({
      ...resume,
      summary: {
        ...(resume.summary || {}),
        content: value,
      },
    });
  };

  const updateSection = (nextSection) => {
    onChange({
      ...resume,
      sections: (resume.sections || []).map((section) =>
        getResumeSectionKey(section) === getResumeSectionKey(nextSection)
          ? nextSection
          : section
      ),
    });
  };

  return (
    <article
      className={outOfBoundary ? "ats-resume ats-resume-out-of-boundary" : "ats-resume"}
      ref={resumeRef}
    >
      <header className="ats-contact">
        <h1>
          <EditableText
            value={contact.name || ""}
            placeholder="Candidate Name"
            onSave={(value) => updateContact("name", value)}
          />
        </h1>

        <p className="ats-contact-line">
          {contactFields.map(([field, value]) => {
            const hasValue = String(value || "").trim() !== "";
            const shouldShowSeparator = hasValue && visibleContactCount > 0;
            if (hasValue) visibleContactCount += 1;

            return (
              <span
                className={
                  hasValue
                    ? "ats-contact-part"
                    : "ats-contact-part empty-contact-part"
                }
                key={field}
              >
                {shouldShowSeparator && (
                  <span className="ats-contact-separator">&bull;</span>
                )}
                <EditableText
                  value={value || ""}
                  placeholder={field}
                  onSave={(nextValue) => updateContact(field, nextValue)}
                />
              </span>
            );
          })}
        </p>
      </header>

      {resume.summary?.visible !== false && (
        <section className="ats-section">
          <h2>Summary</h2>
          <EditableText
            as="p"
            className="ats-summary"
            value={resume.summary?.content || ""}
            placeholder="Summary"
            onSave={updateSummary}
          />
        </section>
      )}

      {sections.map((section) => (
        <EditableResumeSection
          key={getResumeSectionKey(section)}
          section={section}
          onChange={updateSection}
        />
      ))}
    </article>
  );
}
