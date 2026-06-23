# API Endpoints

All non-auth APIs require JWT authentication. Endpoints that expose booking
management data require an approved Admin or Superadmin unless explicitly
documented as Requester-safe.

## Auth

### Admin Signup

```http
POST /api/auth/admin/signup/
```

Creates a pending Admin account. Requires `ADMIN_SIGNUP_CODE`.

Response behavior:

- Valid signup returns user data with `approval_status=pending`.
- No JWT access or refresh token is issued for pending accounts.
- Invalid invite code is rejected with a user-friendly validation error.

### Admin Login

```http
POST /api/auth/admin/login/
```

Allowed for approved Admin accounts. Superadmin may also use this endpoint when
the backend allows Admin-like mobile access.

Rejected cases:

- pending account
- rejected account
- Requester role
- invalid credentials

### Requester Signup

```http
POST /api/auth/requester/signup/
```

Creates a pending Requester account. Does not require Admin invite code.

Response behavior:

- Valid signup returns user data with `approval_status=pending`.
- No JWT access or refresh token is issued for pending accounts.

### Requester Login

```http
POST /api/auth/requester/login/
```

Allowed for approved Requester accounts.

Rejected cases:

- pending account
- rejected account
- Admin or Superadmin role
- invalid credentials

### Current User

```http
GET /api/auth/me/
```

Returns approved authenticated user data:

- id
- name
- email
- role
- approval_status
- department
- designation
- mobile

### Logout

```http
POST /api/auth/logout/
```

Logs out the current user. If refresh-token blacklist support is available,
the refresh token can be blacklisted.

### Token Refresh

```http
POST /api/auth/token/refresh/
```

Returns a new access token for a valid refresh token.

## Superadmin Account Approval

Superadmin endpoints require an approved Superadmin.

```http
GET /api/superadmin/account-requests/
GET /api/superadmin/account-requests/<id>/
POST /api/superadmin/account-requests/<id>/approve/
POST /api/superadmin/account-requests/<id>/reject/
```

Query filters:

- `role=admin`
- `role=requester`
- `status=pending`
- `status=approved`
- `status=rejected`

Superadmin can approve or reject Admin and Requester accounts.

Reject request body:

```json
{
  "remarks": "Optional reason"
}
```

## Admin Requester Account Approval

Admin requester account approval endpoints require approved Admin or approved
Superadmin.

```http
GET /api/admin/requester-accounts/
GET /api/admin/requester-accounts/<id>/
POST /api/admin/requester-accounts/<id>/approve/
POST /api/admin/requester-accounts/<id>/reject/
```

Admins can approve or reject Requester accounts only. Admins cannot approve
Admin or Superadmin accounts.

Reject request body:

```json
{
  "remarks": "Optional reason"
}
```

## Admin Booking Request Review

Admin booking request endpoints require approved Admin or approved Superadmin.

```http
GET /api/admin/booking-requests/
GET /api/admin/booking-requests/<id>/
POST /api/admin/booking-requests/<id>/approve/
POST /api/admin/booking-requests/<id>/reject/
```

Approve request body:

```json
{
  "room": 11,
  "remarks": "Optional approval remarks"
}
```

Approval behavior:

- Re-checks room availability at approval time.
- Creates a real Booking only if the selected room is still available.
- Sets the approving Admin or Superadmin as Created By on the Booking.
- Updates the BookingRequest to `approved`.
- Notifies the Requester.

If the room is no longer available, approval fails with a friendly error and no
Booking is created.

Reject request body:

```json
{
  "remarks": "Optional rejection reason"
}
```

Rejection behavior:

- Does not create a Booking.
- Updates the BookingRequest to `rejected`.
- Stores Admin remarks.
- Notifies the Requester.

## Requester APIs

Requester endpoints require approved Requester.

### Requester Availability

```http
GET /api/requester/availability/?month=6&year=2026
```

Returns requester-safe calendar availability.

Requester availability must not expose:

- visitor names
- requestor names
- Created By
- booking detail records
- edit history

### Booking Requests

```http
POST /api/requester/booking-requests/
GET /api/requester/booking-requests/
GET /api/requester/booking-requests/<id>/
```

Requester can submit a booking request and view only their own requests.

Requester request fields include:

- arrival and departure date/time
- visitor details
- visitor category
- purpose of visit
- attender requirement without charge fields
- room preference
- requester/requestor details

Requester flow does not create a real Booking directly. A real Booking is
created only after Admin approval.

## Admin Booking APIs

The existing booking management APIs are protected for approved Admin or
approved Superadmin only.

Protected areas include:

- rooms
- bookings list
- booking detail
- booking create
- booking edit
- booking delete
- availability calendar
- available rooms
- available rooms by date range

Delete remains permanent delete. There is no cancelled/cancel booking flow.

## Created By and Edit History

Booking create ignores client-sent Created By values. Created By comes from the
authenticated logged-in user.

Booking detail response includes:

- `created_by_name`
- `created_at`
- `edit_history`

Edit history rows include:

- editor name
- editor email
- edited timestamp
- field name
- field label
- old value
- new value

Booking list responses should remain lightweight and must not include full edit
history.

## Budget Head Fields

Backend stores:

- `budget_head_name`
- `budget_head_department_name`
- `budget_head_project_code`

Android Budget Head radio selection is UI/focus behavior only. API mapping
continues to use the three stored fields above.

Google Sheet sync stores only Name/Organisation value as required.
