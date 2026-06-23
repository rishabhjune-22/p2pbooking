# Deployment Notes

These notes describe safe deployment and local development practices for the
Room Booking backend and Android app. Do not place secrets in this file.

## Local Backend Commands

```bash
cd /home/alpha/RoomBooking
source venv/bin/activate
python manage.py check
python manage.py migrate
python manage.py test
python manage.py runserver 127.0.0.1:8000
```

For Android USB testing against the local backend:

```bash
adb reverse tcp:8000 tcp:8000
```

## Local Android Commands

```bash
cd /home/alpha/AndroidStudioProjects/p2pbooking
adb reverse tcp:8000 tcp:8000
./gradlew testDebugUnitTest
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Production Deployment Order

Deploy backend before APK when migrations or APIs changed.

Recommended order:

1. Backup the production database.
2. Deploy backend code.
3. Install backend dependencies if requirements changed.
4. Run Django checks.
5. Run database migrations.
6. Restart backend service.
7. Verify service status and logs.
8. Create or verify production Superadmin.
9. Build and distribute Android APK only after backend compatibility is
   confirmed.

## Required Migrations

The production database must include these migrations for the current role,
audit, and booking request workflow:

- `accounts.0001_initial`
- `accounts.0002_userprofile_approval`
- `bookings.0018_booking_created_by_bookingedithistory`
- `bookings.0019_bookingrequest_usernotification_and_more`
- `bookings.0020_delete_usernotification`

## Production Commands

Run from the backend project directory on the server:

```bash
python manage.py check
python manage.py migrate
sudo systemctl restart room-booking-web.service
```

Then verify:

```bash
sudo systemctl status room-booking-web.service
journalctl -u room-booking-web.service --no-pager
```

Do not run deployment commands against production unless explicitly intended.

## Superadmin Verification

Create or verify a production Superadmin with Django:

```bash
python manage.py createsuperuser
```

Expected profile:

- role: `superadmin`
- approval status: `approved`
- `is_staff=True`
- `is_superuser=True`

Only Superadmin should access Django admin.

Admin and Requester accounts must have:

- `is_staff=False`
- `is_superuser=False`

## Environment Variables

Document variable names only. Do not commit values.

Backend:

- `SECRET_KEY`
- `DEBUG`
- `ALLOWED_HOSTS`
- database engine/name/user/password/host/port settings
- `ADMIN_SIGNUP_CODE`
- SimpleJWT/JWT token lifetime and signing settings
- Google Sheet credentials/settings, if enabled

Android:

- API base URL configuration
- debug/local base URL configuration
- release/prod base URL configuration

Never commit:

- real passwords
- signing keys
- JWT keys
- `.env` values
- private IP credentials
- keystore paths
- Google service account secrets

## Availability Behavior

Calendar tint is intentionally separate from bottom-sheet availability.

Tint logic:

- If a booking touches a date even partially, that date should be tinted.
- A departure date is tinted when a room is occupied earlier that day.

Bottom-sheet behavior:

- Partial departure-day availability remains partial.
- Rooms can still show as available from a later time.
- Do not change cooling-period or booking validation behavior as part of tint
  changes.

## Cache and Background Sync

The four Android reliability/cache phases are complete:

- cached-first loading
- availability cache
- offline sync status
- WorkManager background sync

Production behavior:

- WorkManager Admin sync runs only for approved Admin or approved Superadmin.
- Requester does not call Admin booking sync endpoints.
- Logout clears sensitive session/cache data.

## Budget Head and Google Sheet Sync

Backend stores:

- `budget_head_name`
- `budget_head_department_name`
- `budget_head_project_code`

Android Budget Head radio selection is UI/focus behavior only.

Google Sheet sync stores only Name/Organisation value as required.

## QA Checklist

Backend:

- Superadmin profile verified.
- Admin pending approval flow works.
- Requester pending approval flow works.
- Pending/rejected users cannot login.
- Admin can approve Requester.
- Superadmin can approve Admin.
- Admin cannot approve Admin accounts.
- Requester cannot approve accounts.
- Requester cannot access Admin APIs.
- Admin cannot access Django admin.
- Approved Admin can manage bookings.
- Approved Requester can submit booking requests.
- Admin approve creates a real Booking after availability re-check.
- Admin reject does not create a Booking.
- Backend tests pass.

Android:

- Fresh install opens LoginActivity.
- Admin and Requester tabs are visible.
- No Superadmin tab.
- Pending account message appears.
- Rejected account message appears.
- Approved Admin routes to LandingActivity.
- Approved Requester routes to RequesterLandingActivity.
- Requester calendar is sanitized.
- Requester cannot access Admin screens.
- Calendar tint behavior works.
- Created By prompt is not shown.
- Booking detail shows Created By and Edit History.
- Booking list/cards do not show Edit History.
- No cancelled/cancel booking flow exists.
- Delete remains permanent delete.
- `./gradlew testDebugUnitTest` passes.
- `./gradlew assembleDebug` passes.
- No `FATAL EXCEPTION` appears in logcat.
