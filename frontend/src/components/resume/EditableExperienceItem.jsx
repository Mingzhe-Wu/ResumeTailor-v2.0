import EditableBulletList from "./EditableBulletList.jsx";
import EditableText from "./EditableText.jsx";
import {
  formatDateRange,
  updateBulletField,
  updateDateRangeFields,
  updateFirstExistingField,
} from "./resumeUtils.js";

export default function EditableExperienceItem({ item, onChange }) {
  const title = item.title || item.position || item.role;
  const company = item.company || item.companyName;

  return (
    <div className="ats-item">
      <div className="ats-item-heading">
        <div className="ats-item-title">
          <EditableText
            value={company || ""}
            placeholder="Company"
            onSave={(value) => onChange(updateFirstExistingField(item, ["company", "companyName"], value))}
          />
          <span className="ats-inline-separator"> | </span>
          <span className="ats-experience-position">
            <EditableText
              value={title || ""}
              placeholder="Title"
              onSave={(value) => onChange(updateFirstExistingField(item, ["title", "position", "role"], value))}
            />
          </span>
        </div>
        <span className="ats-item-date">
          <EditableText
            value={formatDateRange(item.startDate, item.endDate)}
            placeholder="Date range"
            onSave={(value) => onChange(updateDateRangeFields(item, value))}
          />
        </span>
      </div>
      <EditableText
        as="p"
        className="ats-meta"
        value={item.location || ""}
        placeholder="Location"
        onSave={(value) => onChange({ ...item, location: value })}
      />
      <EditableBulletList
        bullets={item.bullets || item.details || item.description}
        onSave={(bullets) => onChange(updateBulletField(item, bullets))}
      />
    </div>
  );
}
