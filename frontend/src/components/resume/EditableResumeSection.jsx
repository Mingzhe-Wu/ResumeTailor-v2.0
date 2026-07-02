import EditableEducationItem from "./EditableEducationItem.jsx";
import EditableExperienceItem from "./EditableExperienceItem.jsx";
import EditableProjectItem from "./EditableProjectItem.jsx";
import EditableSkillItem from "./EditableSkillItem.jsx";
import { appendEmptySkillItem, getResumeSectionTitle, isEmptySkillItem } from "./resumeUtils.js";

export default function EditableResumeSection({ section, onChange }) {
  const items = Array.isArray(section.items)
    ? section.items.filter((item) => item.visible !== false)
    : [];
  const type = String(section.type || "").toLowerCase();

  if (items.length === 0 && !type.includes("skill")) return null;

  const updateItem = (item, nextItem) => {
    onChange({
      ...section,
      items: (section.items || [])
        .map((sectionItem) =>
          sectionItem === item || (item.id != null && sectionItem.id === item.id)
            ? nextItem
            : sectionItem
        )
        .filter((sectionItem) => !(type.includes("skill") && isEmptySkillItem(sectionItem))),
    });
  };

  return (
    <section className="ats-section">
      <h2 className="ats-section-heading">
        <span>{section.title || getResumeSectionTitle(type)}</span>
        {type.includes("skill") && (
          <button
            type="button"
            className="ats-add-bullet-button ats-add-skill-button"
            onClick={() => onChange(appendEmptySkillItem(section))}
            aria-label="Add skill category"
            title="Add skill category"
          >
            +
          </button>
        )}
      </h2>

      {type.includes("experience") && items.map((item, index) => (
        <EditableExperienceItem
          key={item.id || index}
          item={item}
          onChange={(nextItem) => updateItem(item, nextItem)}
        />
      ))}

      {type.includes("project") && items.map((item, index) => (
        <EditableProjectItem
          key={item.id || index}
          item={item}
          onChange={(nextItem) => updateItem(item, nextItem)}
        />
      ))}

      {type.includes("education") && items.map((item, index) => (
        <EditableEducationItem
          key={item.id || index}
          item={item}
          onChange={(nextItem) => updateItem(item, nextItem)}
        />
      ))}

      {type.includes("skill") && items.map((item, index) => (
        <EditableSkillItem
          key={item.id || index}
          item={item}
          onChange={(nextItem) => updateItem(item, nextItem)}
        />
      ))}
    </section>
  );
}
