# Android Flow and QA

Android package:

```text
com.example.roombooking
```

## Launch and Routing

Fresh launch starts at SplashActivity.

Routing:

- logged out -> LoginActivity
- approved Admin -> LandingActivity
- approved Superadmin -> LandingActivity, if Admin-like mobile access is used
- approved Requester -> RequesterLandingActivity
- pending or rejected account -> LoginActivity with friendly message

## Login UI

LoginActivity has two tabs:

- Admin
- Requester

There is no Superadmin tab.

Admin tab:

- email
- password
- login
- signup link

Requester tab:

- email
- password
- login
- signup link

Pending login shows:

```text
Your account is pending approval.
```

Rejected login shows a rejected account message and reason when returned by the
backend.

## Signup UI

Admin signup:

- full name
- email
- password
- confirm password
- Admin invite code

Successful Admin signup shows a pending Superadmin approval message. Android
does not auto-login the pending Admin account.

Requester signup:

- full name
- email
- password
- confirm password
- optional requester profile details such as department, designation, and mobile

Successful Requester signup shows a pending approval message. Android does not
auto-login the pending Requester account.

## Admin UI

Approved Admin lands on the existing Admin app flow.

Admin UI includes:

- calendar
- booking list
- booking create
- booking edit
- booking detail
- Booking Requests
- Requester Accounts
- Logout

Admin can:

- manage bookings
- view Created By and Created At
- view Edit History on booking detail
- approve or reject Requester accounts
- approve or reject booking requests

## Requester UI

Approved Requester lands on RequesterLandingActivity.

Requester UI includes:

- Requester Availability
- Request Booking
- My Requests
- Logout

Requester can:

- view sanitized calendar availability
- select a date range
- submit a booking request
- view own requests and status
- view Admin remarks

Requester cannot see:

- Admin booking cards
- booking list of all bookings
- booking details of other users
- visitor names from other bookings
- requestor/admin names from other bookings
- Created By for other bookings
- edit history for bookings

## Request Booking Form

Requester request form includes:

- visitor details
- visitor category
- purpose of visit
- attender requirement without charges
- room preference
- requester/requestor details

Requester request form does not include:

- room charges
- attender charges

## Cache and Background Sync

The existing reliability/cache phases are completed:

- cached-first loading
- availability cache
- offline sync status
- WorkManager background sync

WorkManager behavior:

- logged out -> skip protected sync
- approved Admin -> run Admin sync
- approved Superadmin -> run Admin sync if Admin-like mobile access is used
- Requester -> do not call Admin booking sync endpoints
- pending or rejected account -> do not enter app or run protected sync

Logout clears sensitive session/cache data so one user does not see another
user's private booking data.

## Availability Behavior

Admin sees full booking and availability management.

Requester sees sanitized calendar availability only.

Partial/tinted calendar logic:

- If any room is occupied for any part of a date, that date is tinted.
- A departure date is tinted when the room was occupied earlier that day.
- Calendar tint does not make the room unavailable for the entire departure day.
- Bottom sheet behavior remains partial: it can show that a room is available
  from a later time on that date.

## Audit History UI

Booking detail shows:

- Created By
- Created At
- Edit History

Edit History empty state:

```text
No edits recorded yet.
```

Edit History rows show:

- who edited
- timestamp
- changed field
- old value
- new value

Edit History is not shown on booking cards or booking lists.

## Budget Head UI

Backend stores:

- `budget_head_name`
- `budget_head_department_name`
- `budget_head_project_code`

Android Budget Head radio selection is UI/focus behavior only.

Expected behavior:

- no Budget Head radio option selected by default
- selecting a radio option focuses the matching input
- Clear Selection clears the selected radio and related field when configured
- saving without changes should not create false audit rows

## Local Android Commands

```bash
cd /home/alpha/AndroidStudioProjects/p2pbooking
adb reverse tcp:8000 tcp:8000
./gradlew testDebugUnitTest
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Manual QA Checklist

Login and approval:

- Fresh install opens LoginActivity.
- Login screen has Admin and Requester tabs only.
- No Superadmin tab.
- Admin signup with wrong invite code shows friendly error.
- Admin signup with correct invite code shows pending Superadmin approval.
- Pending Admin login shows pending approval message.
- Approved Admin login routes to LandingActivity.
- Requester signup shows pending approval.
- Pending Requester login shows pending approval message.
- Admin approves Requester from Requester Accounts.
- Approved Requester login routes to RequesterLandingActivity.

Booking request flow:

- Requester sees sanitized calendar.
- Requester does not see who booked rooms.
- Requester submits booking request.
- My Requests shows Pending.
- Admin sees pending booking request.
- Admin approves with room and remarks.
- Requester sees Approved status and remarks.
- Admin rejects another request with remarks.
- Requester sees Rejected status and remarks.
- Rejected request does not create a real Booking.

Role guards:

- Requester cannot open Admin screens through normal UI.
- Requester cannot access Admin APIs.
- Admin cannot access Django admin.
- WorkManager Admin sync runs only for approved Admin or approved Superadmin.
- Pending/rejected accounts do not enter app.

Regression checks:

- Calendar tint behavior works for partial departure-date bookings.
- No cancelled/cancel booking flow exists.
- Delete remains permanent delete.
- Created By prompt is not shown.
- Booking detail shows Created By and Edit History.
- Booking cards/lists do not show Edit History.
- Android unit tests pass.
- Debug APK builds.
- No `FATAL EXCEPTION` appears in logcat.
