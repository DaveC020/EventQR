CREATE OR REPLACE FUNCTION public.handle_transaction_log_effects()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $function$
BEGIN
  IF NEW.transaction_result = 'APPROVED' THEN
    IF NEW.transaction_type = 'ENTRY' THEN
      UPDATE public.event_registrations
      SET status = 'ENTERED',
          entered_at = COALESCE(entered_at, COALESCE(NEW.scanned_at, now())),
          checked_in_by_user_id = NEW.staff_user_id,
          updated_at = now()
      WHERE id = NEW.registration_id;
    ELSIF NEW.transaction_type = 'ATTENDANCE' THEN
      UPDATE public.event_registrations
      SET attended_at = COALESCE(attended_at, COALESCE(NEW.scanned_at, now())),
          checked_in_by_user_id = NEW.staff_user_id,
          updated_at = now()
      WHERE id = NEW.registration_id;
    ELSIF NEW.transaction_type = 'EXIT' THEN
      UPDATE public.event_registrations
      SET status = 'EXITED',
          exited_at = COALESCE(exited_at, COALESCE(NEW.scanned_at, now())),
          updated_at = now()
      WHERE id = NEW.registration_id;
    END IF;
  END IF;

  RETURN NEW;
END;
$function$;
