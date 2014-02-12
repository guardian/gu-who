Steps for the bot
-----------------

Get a full list of members of our org, and find 'problem' users.

Get the full list of Open issues in the 'people' repo *that have been opened by our bot*
(GET /repos/:owner/:repo/issues?creator=gu-who-bot&state=open - note that the 'people' repo
also contains pull-requests, which appear as issues within the GitHub API, so we have to be
careful we don't interact with those) 

Create an issue (like https://github.com/guardian/people/issues/13) for any user that doesn't
have one yet - the username should be in the subject, and the issue should be assigned to that user.

For each open issue, delta the list of labels the Issue has with what it should have
('TwoFactorAuth', 'FullName', 'Sponsor'). Remove/Add labels as appropriate
(only remove 'our' labels, not different ones that humans may have added, like 'UselessAvatar'),
and for bonus points, add a nice comment on the Issue describing why the label was added or removed,
and what problems the user still needs to address. Feel free to use good emoji like :sparkles: or whatever.

If an issue that we created is open against a user, but the user has no problems and there are no other
labels on the issue, we should then close the issue.

* Before adding an issue against the user, ensure they are a member of the 'all' team
(GET /teams/:id/members/:user) - otherwise they won't get the Issue notification.

* Before adding any user to the 'all' Team, we must always check the user is STILL in the Org
(GET /orgs/:org/members/:user) to ensure we don't add a recently _removed_ user.

