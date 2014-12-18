GU: Who?
========

_answering: "Who has access to my GitHub organisation - and why?"_

_gu:who?_ is a simple service for auditing the members of your GitHub
[organisation](https://github.com/blog/674-introducing-organizations).
It was written by The Guardian to get their 200-strong GitHub
organisation under control, resulting in 100% of membership being
accounted for and **98%** Two-Factor-Auth enabled, up from 54% -
you can read more about it in this
[Guardian Developers blogpost](http://www.theguardian.com/info/developer-blog/2014/apr/11/how-the-guardian-uses-github-to-audit-github).

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

**It does this by using _GitHub_ as its user-interface:**
GitHub issues and simple text files stored in GitHub 'people' repo held
under your org- no other database or spreadsheet, no Active Directory
or LDAP.

Just the tools the developer already uses: GitHub

Enforced Requirements
--------

These requirements are intended to make it easier to manage the user
accounts and work out if they should be in your organisation or not:

* **Two-Factor-Auth** enabled (this requirement can be waived for users in the 'bots' team - for instance, for a long-lived CI bot account that may need to be accessed by multiple humans, who would otherwise have to share an authentication token)
* A **Full Name** set in the user GitHub Profile
* **Sponsor**: each GitHub username should be in https://github.com/guardian/people/blob/master/users.txt - added by Pull Request by any senior member of the organisation (who, in effect, acts as the 'sponsor' for the user for being in the GitHub Org). The current GitHub admin interface doesn't give any long-term audit trail on how a user came to join an Org, so this file serves that purpose.


Actions taken by the gu-who bot...
----------------------------------

* Opens a GitHub issue against each user that doesn't pass the requirements
* Conceals organisation membership for users which don't comply with the requirements
* After a grace period, removes insecure users from the org


What's your logo?
-----------------

Well, obviously, it would be the ridiculously suitable 
**[Riddlocat](https://octodex.github.com/images/riddlocat.png)** by [@cameronmcefee](https://github.com/cameronmcefee), but we can't use
it for legal reasons laid out so eloquently by the Riddlocat _himself_
on the [GitHub Octodex FAQ](https://octodex.github.com/faq.html).

You'll just have to imagine the logo there.

What else?
----------

If you're interested in Git and security, you may also be interested in
[The BFG Repo-Cleaner](http://rtyley.github.io/bfg-repo-cleaner/), a 
simpler, [faster](http://youtu.be/Ir4IHzPhJuI) alternative to
`git-filter-branch` for cleansing bad data out of your Git repository -
ie **Passwords, Credentials** & other private or unwanted data.

If you do frequent or continuous deployment, try
[**prout**](https://github.com/guardian/prout#prout) - _"is your pull request out yet?"_

