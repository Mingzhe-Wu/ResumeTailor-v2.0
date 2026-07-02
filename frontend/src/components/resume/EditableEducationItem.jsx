import EditableBulletList from "./EditableBulletList.jsx";
import EditableText from "./EditableText.jsx";
import {
  appendEmptyBulletField,
  formatDateRange,
  updateBulletField,
  updateEducationDetailFields,
  updateEducationMetaFields,
  updateFirstExistingField,
} from "./resumeUtils.js";

export default function EditableEducationItem({ item, onChange }) {
  const school = item.school || item.schoolName;
  const degreeLine = [item.degree, item.major].filter(Boolean).join(", ");
  const dateLine = [item.location, formatDateRange(item.startDate, item.endDate)]
    .filter(Boolean)
    .join(" | ");
  const detailLine = [degreeLine, item.gpa ? `GPA: ${item.gpa}` : ""]
    .filter(Boolean)
    .join(" | ");

  return (
    <div className="ats-item">
      <div className="ats-item-heading">
        <strong className="ats-item-title">
          <EditableText
            value={school || ""}
            placeholder="School"
            onSave={(value) => onChange(updateFirstExistingField(item, ["school", "schoolName"], value))}
          />
          <button
            type="button"
            className="ats-add-bullet-button"
            onClick={() => onChange(appendEmptyBulletField(item))}
            aria-label="Add bullet to this education"
            title="Add bullet"
          >
            +
          </button>
        </strong>
        <span className="ats-item-date">
          <EditableText
            value={dateLine}
            placeholder="Location | Date range"
            onSave={(value) => onChange(updateEducationMetaFields(item, value))}
          />
        </span>
      </div>
      <EditableText
        as="p"
        className="ats-meta"
        value={detailLine}
        placeholder="Degree, Major | GPA"
        onSave={(value) => onChange(updateEducationDetailFields(item, value))}
      />
      <EditableBulletList
        bullets={item.details || item.relevantCoursework || item.description}
        onSave={(bullets) => onChange(updateBulletField(item, bullets))}
      />
    </div>
  );
}
