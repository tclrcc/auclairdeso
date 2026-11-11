-- An optional presentation video per offering, with its transcript (docs/specification.md, 3.6).
-- The file itself is never in the database nor in Git: it lives in MEDIA_DIR on the VPS and is served by nginx
-- under /contenus/. video_path is only its name, relative to that folder.
ALTER TABLE offering ADD COLUMN video_path VARCHAR(100);
ALTER TABLE offering ADD COLUMN video_transcript TEXT;

-- Same shape as the check of the application (OfferingDraft): a plain file name, never a path.
ALTER TABLE offering ADD CONSTRAINT offering_video_path_ck
    CHECK (video_path ~ '^[a-z0-9][a-z0-9-]*\.(mp4|webm)$');
ALTER TABLE offering ADD CONSTRAINT offering_video_transcript_ck
    CHECK (char_length(video_transcript) <= 4000);
