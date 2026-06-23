# Authentication, Roles, and Approval

## Roles

### Superadmin

Superadmin is created through Django `createsuperuser`.

Capabilities:

- Can log in to Django admin.
- Can approve or reject Admin accounts.
- Can approve or reject Requester accounts.
- Can view and manage everything.
- Can use Admin-like backend APIs where allowed.

Django flags:

- `is_staff=True`
- `is_superuser=True`

Superadmin does not have a separate Android login tab. If supported by the
backend, Superadmin can use the Android Admin tab as an admin-like user.

### Admin

Admin uses the Android Admin tab.

Capabilities:

- Can approve or reject Requester accounts.
- Can approve or reject booking requests.
- Can manage rooms/bookings through the Admin app flow.
- Can view booking details, Created By, Created At, and Edit History.

Restrictions:

- Cannot access Django admin.
- Cannot approve Admin accounts.
- Cannot approve Superadmin accounts.
- Cannot use Requester-only workflow unless logged in as a Requester account.

Django flags:

- `is_staff=False`
- `is_superuser=False`

### Requester

Requester uses the Android Requester tab.

Capabilities:

- Can view requester-safe calendar availability.
- Can submit booking requests.
- Can view only own booking request status.
- Can see Admin remarks on approved or rejected requests.

Restrictions:

- Cannot access Django admin.
- Cannot access Admin booking list, cards, or booking details.
- Cannot see who booked a room.
- Cannot see visitor names, requestor names, Created By, or edit history for
  other bookings.
- Cannot approve accounts or booking requests.

Django flags:

- `is_staff=False`
- `is_superuser=False`

## Account Approval Status

Each authenticated user has a role and a separate approval status.

Approval statuses:

- `pending`
- `approved`
- `rejected`

Pending and rejected users do not receive JWT tokens from login endpoints.

### Admin Signup

Admin signup requires `ADMIN_SIGNUP_CODE`.

Flow:

1. User signs up through Android Admin signup.
2. Backend validates the invite code.
3. Backend creates `role=admin` with `approval_status=pending`.
4. Superadmin approves or rejects the Admin account.
5. Approved Admin can log in through the Android Admin tab.

Even when the invite code is valid, the Admin account remains pending until
Superadmin approval.

### Requester Signup

Flow:

1. User signs up through Android Requester signup.
2. Backend creates `role=requester` with `approval_status=pending`.
3. Admin or Superadmin approves or rejects the Requester account.
4. Approved Requester can log in through the Android Requester tab.

Requester signup does not require the Admin invite code.

### Pending and Rejected Login Behavior

Pending users:

- Cannot log in.
- Login response returns a friendly pending approval message.
- Android stays on LoginActivity and shows the pending message.

Rejected users:

- Cannot log in.
- Login response returns a rejected account message.
- Rejection reason is returned when available and safe to show.

Role mismatch:

- Admin login rejects Requester accounts.
- Requester login rejects Admin accounts.
- Superadmin may be accepted by the Admin login path when backend permissions
  treat Superadmin as Admin-like.

## Android Routing

SplashActivity checks the local auth session:

- Logged out -> LoginActivity
- Approved Admin -> LandingActivity
- Approved Superadmin -> LandingActivity, if Admin-like mobile access is used
- Approved Requester -> RequesterLandingActivity
- Pending or rejected session -> clear/deny session and return to LoginActivity

LoginActivity has two tabs only:

- Admin
- Requester

There is no Superadmin Android tab.

## Session Storage and Guards

Android stores:

- access token
- refresh token
- user id
- name
- email
- role
- approval status

Role helpers:

- `isApproved()`
- `isSuperadmin()`
- `isAdmin()`
- `isRequester()`
- `isAdminLike()`

Admin-only screens require approved Admin or approved Superadmin. Requester-only
screens require approved Requester.

## Audit History

Created By comes from the logged-in Django user, not from Android input.

Booking detail shows:

- Created By
- Created At
- Edit History

Edit History includes:

- editor
- timestamp
- changed field
- old value
- new value

Edit History appears only on booking detail. It is not shown on booking cards
or booking lists.
