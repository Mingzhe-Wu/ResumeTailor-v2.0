export default function EditableText({ value, onSave, placeholder = "", as: Tag = "span", className = "", multiline = false }) {
  const displayValue = value || "";

  if (multiline) {
    return (
      <textarea
        className={`ats-editable ats-editable-textarea ${className}`}
        value={displayValue}
        placeholder={placeholder}
        onChange={(e) => onSave(e.target.value)}
      />
    );
  }

  return (
    <Tag
      className={`ats-editable ${className}`}
      contentEditable
      suppressContentEditableWarning
      data-placeholder={placeholder}
      onBlur={(e) => onSave(e.currentTarget.textContent.trim())}
    >
      {displayValue}
    </Tag>
  );
}
