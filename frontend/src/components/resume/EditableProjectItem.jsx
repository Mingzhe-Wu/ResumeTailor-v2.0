import EditableBulletList from "./EditableBulletList.jsx";
import EditableText from "./EditableText.jsx";
import { getProjectHref } from "./resumeLinkUtils.js";
import {
  appendEmptyBulletField,
  formatDateRange,
  formatDelimitedList,
  parseDelimitedListLike,
  updateBulletField,
  updateDateRangeFields,
  updateFirstExistingField,
} from "./resumeUtils.js";

export default function EditableProjectItem({ item, onChange }) {
  const name = item.name || item.projectName;
  const techStack = formatDelimitedList(item.techStack, " \u2022 ");

  return (
    <div className="ats-item">
      <div className="ats-item-heading">
        <strong className="ats-item-title">
          <EditableText
            value={name || ""}
            placeholder="Project name"
            href={getProjectHref(item)}
            onSave={(value) => onChange(updateFirstExistingField(item, ["name", "projectName"], value))}
          />
          <button
            type="button"
            className="ats-add-bullet-button"
            onClick={() => onChange(appendEmptyBulletField(item))}
            aria-label="Add bullet to this project"
            title="Add bullet"
          >
            +
          </button>
        </strong>
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
        value={techStack}
        placeholder="Tech stack"
        onSave={(value) => onChange({
          ...item,
          techStack: parseDelimitedListLike(item.techStack, value, "\u2022"),
        })}
      />
      <EditableBulletList
        bullets={item.bullets || item.details || item.description}
        onSave={(bullets) => onChange(updateBulletField(item, bullets))}
      />
    </div>
  );
}
