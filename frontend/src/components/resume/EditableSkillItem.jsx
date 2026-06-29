import EditableText from "./EditableText.jsx";
import {
  formatDelimitedList,
  getSkillFieldName,
  parseDelimitedListLike,
} from "./resumeUtils.js";

export default function EditableSkillItem({ item, onChange }) {
  const skills = item.skills || item.names || item.items || item.name;
  const skillText = formatDelimitedList(skills, ", ");

  return (
    <p className="ats-skill-line">
      <strong>
        <EditableText
          value={item.category || ""}
          placeholder="Category"
          onSave={(value) => onChange({ ...item, category: value })}
        />
        {": "}
      </strong>
      <EditableText
        value={skillText}
        placeholder="skill1, skill2, skill3"
        onSave={(value) => onChange({
          ...item,
          [getSkillFieldName(item)]: parseDelimitedListLike(skills, value, ","),
        })}
      />
    </p>
  );
}
