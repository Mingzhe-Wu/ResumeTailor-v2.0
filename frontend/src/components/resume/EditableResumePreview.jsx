import EditableResumeSection from "./EditableResumeSection.jsx";
import EditableText from "./EditableText.jsx";
import { getResumeSectionKey, normalizeBullets } from "./resumeUtils.js";

export default function EditableResumePreview({
  resume,
  onChange,
  resumeRef,
  outOfBoundary = false,
  contentTopOffset = 0,
}) {
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

  const additionalInfo = normalizeBullets(contact.additionalInfo, { includeEmpty: true });

  const updateContact = (field, value) => {
    onChange({
      ...resume,
      contact: {
        ...(resume.contact || {}),
        [field]: value,
      },
    });
  };

  const updateAdditionalInfo = (index, value) => {
    const nextAdditionalInfo = [...additionalInfo];
    nextAdditionalInfo[index] = value;
    updateContact(
      "additionalInfo",
      nextAdditionalInfo.map((item) => item.trim()).filter(Boolean)
    );
  };

  const contactFields = [
    ["location", contact.location, "location", (value) => updateContact("location", value)],
    ["email", contact.email, "email", (value) => updateContact("email", value)],
    ["phone", contact.phone, "phone", (value) => updateContact("phone", value)],
    ["linkedin", contact.linkedin, "linkedin", (value) => updateContact("linkedin", value)],
    ["github", contact.github, "github", (value) => updateContact("github", value)],
    ...additionalInfo.map((value, index) => [
      `additional-info-${index}`,
      value,
      "additional info",
      (nextValue) => updateAdditionalInfo(index, nextValue),
    ]),
  ];
  let visibleContactCount = 0;

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
      style={{ "--resume-content-top-offset": `${contentTopOffset}px` }}
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
          {contactFields.map(([field, value, placeholder, onSave]) => {
            const hasValue = String(value || "").trim() !== "";
            // Separators are calculated from non-empty values so exported PDFs
            // do not show orphan bullets for blank editable contact fields.
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
                  placeholder={placeholder}
                  onSave={onSave}
                />
              </span>
            );
          })}
          <button
            type="button"
            className="ats-add-bullet-button ats-add-contact-button"
            onClick={() => updateContact("additionalInfo", [...additionalInfo, ""])}
            aria-label="Add contact information"
            title="Add contact information"
          >
            +
          </button>
        </p>
      </header>

      {resume.summary?.visible !== false && (
        <section className="ats-section">
          <h2 className="ats-section-heading">
            <span>Summary</span>
          </h2>
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
