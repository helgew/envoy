#! /usr/bin/perl
# Simple Perl script to retrieve all historical data from the Enlighten API starting at a specific date
# or at the last date it was run.

use LWP::UserAgent;
use Getopt::Long;
use File::Path qw(make_path);
use POSIX qw(ceil strftime);

require 'settings.pl'; # should include $systemId, $apiKey, and $userId

my $ua = LWP::UserAgent->new();

my $url = "https://api.enphaseenergy.com/api/v2/systems/$systemId/stats?key=$apiKey&user_id=$userId";

my $startTime = 1379425200;
my $endTime = time();
my $outDir = ".";
GetOptions ("start-time=i" => \$startTime,
	    "end-time=i"   => \$endTime,
	    "out-dir=s"    => \$outDir    ) or die "Error in command line args!\n";

# not a lot of checking here
if (! -e $outDir) {
	make_path($outDir, {
             verbose => 1,
             mode => 0711,
	});
}

my $secsPerDay = 24 * 60 * 60;
my $days = ceil(($endTime - $startTime) / $secsPerDay);

my $throttle = 0;
if ($days > 10) { $throttle = 6; } # we can only do 10 calls/min.

my ($start, $file);

for ($i = 0; $i < $days; $i++) {
	$start = $startTime + $i * $secsPerDay;
	my $response = $ua->get($url . "&start_at=$start");

        if ($response->is_success) {
		$file = strftime "$outDir/%F.json", localtime($start);
		print STDERR "writing stats to $file\n";
		open OUT, ">$file" or die "Cannot write to $file: $!\n";
		print OUT $response->decoded_content;
		close OUT;
        } else {
		warn $response->status_line;
        }

	sleep $throttle;
}
