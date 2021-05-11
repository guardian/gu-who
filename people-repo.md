The `people` repository
=======================

In order for _gu:who_ to run against a GitHub organisation, the organisation
must have a repository named `people`. As an example, here's the
`people` repo in the [gu-who-demo-org](https://github.com/gu-who-demo-org):

https://github.com/gu-who-demo-org/people

Effectively, this repository will act as the datastore for _gu:who_:

* It contains the `users.txt` file, listing all the _valid_ members of the
  organisation by username, as manually added by senior staff.
* The GitHub Issues for that project are created by _gu:who_ itself -
  it's where _gu:who_ reports the list of problem users it's found.

Setting up the `people` repository
----------------------------------

Creating the `people` repository is something you need to do manually-
_gu:who_ won't do it for you, and the reason for that is that ideally it
should be a _private_ GitHub repository, given that it will contain
sensitive information. However, the format of the repository is pretty
simple, so it's not hard to setup:

1. Create a new (private) repo (https://github.com/new) - you may as
   well use GitHub's option to initialize this repository with a README.
2. Create a file called `users.txt` at the top-level of the repo's
   file hierarchy, just like this one: https://github.com/gu-who-demo-org/people/blob/master/users.txt

That's enough to get _gu:who_ running. Note that `gu:who` will raise
issues against users in your organisation who don't have an entry in
the `users.txt` file - they should get their line-manager, or even just
a more-senior colleague, to create a pull-request to add their GitHub
username to this file. This is better than just adding usernames in bulk
(ie without vetting them first), because the person who adds that username
to that file is _responsible_ for that person being in the organisation-
and we can see who that is just by using [`git blame`](https://github.com/gu-who-demo-org/people/blame/main/users.txt)


