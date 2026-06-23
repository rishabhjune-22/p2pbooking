# Room Booking App

Room Booking is a Django + Django REST Framework backend with an Android
client for visitor room booking at institute facilities.

The current implementation includes:

- Android package: `com.example.roombooking`
- JWT authentication with SimpleJWT
- Three-role account model: Superadmin, Admin, Requester
- Account approval before Admin or Requester login
- Admin booking management
- Requester booking request workflow
- Created By and booking edit audit history
- Cached-first loading, availability cache, offline sync status, and
  WorkManager background sync

This repository contains the Android app. The paired backend lives locally at:

```bash
/home/alpha/RoomBooking
```

## Documentation

- [Auth and Roles](docs/AUTH_ROLES.md)
- [API Endpoints](docs/API_ENDPOINTS.md)
- [Android QA](docs/ANDROID_QA.md)
- [Deployment Notes](docs/DEPLOYMENT_NOTES.md)

## Project Overview

The system supports two mobile login flows and one administrative web/Django
admin flow:

- Superadmin is created with Django `createsuperuser` and manages account
  approvals from Django admin or backend approval APIs.
- Admin uses the Android Admin tab to manage bookings, requester accounts,
  booking requests, and the calendar.
- Requester uses the Android Requester tab to view sanitized availability,
  submit booking requests, and view own request status.

JWT tokens are issued only to approved accounts. Pending and rejected accounts
cannot enter the Android app.

## Core Workflows

Admin account approval:

1. Admin signs up with `ADMIN_SIGNUP_CODE`.
2. Backend creates a pending Admin account.
3. Superadmin approves or rejects the Admin account.
4. Approved Admin can log in through the Android Admin tab.

Requester account approval:

1. Requester signs up.
2. Backend creates a pending Requester account.
3. Admin or Superadmin approves or rejects the Requester account.
4. Approved Requester can log in through the Android Requester tab.

Booking request approval:

1. Requester selects dates and submits a booking request.
2. Admin reviews the pending request.
3. Admin approves with a final room and optional remarks, or rejects with
   optional remarks.
4. Approval re-checks room availability and creates a real Booking.
5. Rejection does not create a Booking.
6. Requester sees the updated request status in My Requests.

## Local Development

Backend:

```bash
cd /home/alpha/RoomBooking
source venv/bin/activate
python manage.py check
python manage.py migrate
python manage.py test
python manage.py runserver 127.0.0.1:8000
```

Android:

```bash
cd /home/alpha/AndroidStudioProjects/p2pbooking
adb reverse tcp:8000 tcp:8000
./gradlew testDebugUnitTest
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Safety Rules

- Do not re-add cancelled/cancel booking flow.
- Delete remains permanent delete.
- Keep Android `applicationId` as `com.example.roombooking`.
- Do not store real secrets, passwords, signing keys, JWT keys, private IP
  credentials, or keystore paths in documentation.
- Deploy backend migrations and API changes before installing an APK that
  depends on them.
