import EditableText from "./EditableText.jsx";
import { normalizeBullets } from "./resumeUtils.js";

export default function EditableBulletList({ bullets, onSave }) {
  const normalizedBullets = normalizeBullets(bullets);

  if (normalizedBullets.length === 0) return null;

  return (
    <ul className="ats-bullets">
      {normalizedBullets.map((bullet, index) => (
        <li key={index}>
          <EditableText
            value={bullet}
            onSave={(value) => {
              const nextBullets = [...normalizedBullets];
              nextBullets[index] = value;
              onSave(nextBullets);
            }}
          />
        </li>
      ))}
    </ul>
  );
}
