-- Allow DROPPED status when a team voluntarily leaves an event after approval.

ALTER TABLE `team_registrations`
  DROP CHECK `chk_tr_status`;

ALTER TABLE `team_registrations`
  ADD CONSTRAINT `chk_tr_status` CHECK (
    `status` IN ('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED', 'DROPPED')
  );
