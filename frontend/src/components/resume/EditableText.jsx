export default function EditableText({
  value,
  onSave,
  placeholder = "",
  as: Tag = "span",
  className = "",
  multiline = false,
  href,
}) {
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

  const RenderTag = href ? "a" : Tag;

  return (
    <RenderTag
      className={`ats-editable ${href ? "ats-editable-link" : ""} ${className}`}
      contentEditable
      suppressContentEditableWarning
      data-placeholder={placeholder}
      href={href}
      onClick={href ? (event) => event.preventDefault() : undefined}
      onBlur={(e) => onSave(e.currentTarget.textContent.trim())}
    >
      {displayValue}
    </RenderTag>
  );
}
