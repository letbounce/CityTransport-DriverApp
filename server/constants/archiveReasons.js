/** Підтверджені коди причини архівування (видалення в застосунку). */
const ARCHIVE_REASON_CODES = [
  "mistaken_creation",
  "duplicate_entry",
  "no_longer_relevant",
  "entered_in_error",
  "other"
];

function isValidArchiveReason(code) {
  return typeof code === "string" && ARCHIVE_REASON_CODES.includes(code);
}

module.exports = { ARCHIVE_REASON_CODES, isValidArchiveReason };
