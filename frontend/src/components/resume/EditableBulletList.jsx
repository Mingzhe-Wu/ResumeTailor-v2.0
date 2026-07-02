import EditableText from "./EditableText.jsx";
import { normalizeBullets } from "./resumeUtils.js";

export default function EditableBulletList({ bullets, onSave }) {
  const normalizedBullets = normalizeBullets(bullets, { includeEmpty: true });

  if (normalizedBullets.length === 0) return null;

  return (
    <ul className="ats-bullets">
      {normalizedBullets.map((bullet, index) => (
        <li key={index} className={!bullet ? "ats-bullet-empty" : ""}>
          <EditableText
            value={bullet}
            onSave={(value) => {
              const nextBullets = [...normalizedBullets];
              nextBullets[index] = value;
              onSave(nextBullets);
            }}
            placeholder="New bullet"
          />
        </li>
      ))}
    </ul>
  );
}
