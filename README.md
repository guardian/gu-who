GU: Who?
========

_answering: "Who has access to my GitHub organisation - and why?"_

_gu:who?_ is a simple service for auditing the members of your GitHub
[organisation](https://github.com/blog/674-introducing-organizations).

If your organisation is large - and you have 3rd parties, contractors,
etc who you need to give access to your code - it can be very difficult
to work out whether some accounts are _legitimately_ members of
your GitHub organisation or not. Accounts which don't have many details
set in their profile are difficult to identify. When employees leave,
how sure are you that you'll remember to remove their account?

_gu:who?_ aims to make dealing with this all a little bit more easy...
it aims to ensure all users in your organisation meet some basic
requirements, and it makes it easy to see where requirements aren't
being met.

**It does this by using _GitHub_ as it's user-interface:**
GitHub issues and simple text files stored in GitHub 'people' repo held
under your org- no other database or spreadsheet, no Active Directory
or LDAP.

Just the tools the developer already uses: GitHub

Enforced Requirements
--------

These requirements are intended to make it easier to manage the user
accounts and work out if they should be in your organisation or not:

* Two-Factor-Auth enabled
* A full name set in the user GitHub Profile
* Sponsor: each GitHub username should be in https://github.com/guardian/people/blob/master/users.txt - added by Pull Request by any senior member of the organisation (who, in effect, acts as the 'sponsor' for the user for being in the GitHub Org). The current GitHub admin interface doesn't give any long-term audit trail on how a user came to join an Org, so this file serves that purpose.


Actions taken by the gu-who bot...
----------------------------------

* Opens a GitHub issue against each user that doesn't pass the requirements
* Conceals organisation membership for users which don't comply with the requirements
* TODO: After a grace period, removes insecure users from the org
