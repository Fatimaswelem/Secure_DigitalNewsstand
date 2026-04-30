-- ============================================================
--  Digital Newsstand — updated schema for Java backend
--  Key change: userPassword VARCHAR(20) → VARCHAR(60)
--  BCrypt hashes are always exactly 60 characters.
-- ============================================================

-- Drop old table if migrating from PHP version
ALTER TABLE `user`
  MODIFY `userPassword` VARCHAR(60) NOT NULL;

-- If creating from scratch, use this definition for the user table:
--
-- CREATE TABLE `user` (
--   `userId`     INT(11)      NOT NULL AUTO_INCREMENT,
--   `userName`   VARCHAR(50)  NOT NULL,
--   `userEmail`  VARCHAR(100) NOT NULL,
--   `userPassword` VARCHAR(60) NOT NULL,   -- BCrypt hash (60 chars)
--   `userRole`   INT(11)      NOT NULL DEFAULT 2,
--   `languageId` INT(11)      NOT NULL DEFAULT 1,
--   PRIMARY KEY (`userId`),
--   UNIQUE KEY `userEmail` (`userEmail`),
--   FOREIGN KEY (`userRole`)   REFERENCES `role`(`roleId`)     ON UPDATE CASCADE,
--   FOREIGN KEY (`languageId`) REFERENCES `language`(`languageId`) ON UPDATE CASCADE
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
--  Migrate existing plain-text passwords (run once)
-- ============================================================
-- WARNING: After running this script, use the Java endpoint
--   POST /api/admin/migrate-passwords
-- or manually re-register all users, because plain-text
-- passwords cannot be retroactively hashed without knowing
-- the originals.
-- The admin account should be reset first:
--   UPDATE user SET userPassword = '<bcrypt_hash_of_new_password>'
--   WHERE userId = 1;
