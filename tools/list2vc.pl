#!/usr/bin/env perl



package main;
use strict;
use warnings;

our @ARGV;

sub shorten ($) {
  my $line = shift;
  if (length($line) < 20) {
    return $line;
  }
  else {
    return substr($line,0,17) . '...';
  };
};


unless (@ARGV) {
  print <<'HELP';
Convert a line-separated list of corpus sigles, doc sigles or
text sigles into a virtual corpus query.

  $ perl list2vc.pl my_vc.txt | gzip -vc > my_vc.jsonld.gz
  $ cat my_vc.txt | perl list2vc.pl - | gzip -vc > my_vc.jsonld.gz

HELP
exit 0;
};

my $fh;
if ($ARGV[0] eq '-') {
  $fh = *STDIN;
} elsif (!open($fh, '<' . $ARGV[0])) {
  warn $ARGV[0] . " can't be opened";
  exit(0);
};


my %data = (
  corpus => [],
  doc => [],
  text => []
);

# Iterate over the whole list
while (!eof $fh) {
  my $line = readline($fh);
  chomp $line;


  # Skip empty lines
  if (!$line || length($line) == 0 || $line =~ /^[\s\t\n]*$/) {
    # empty
    next;
  };

  # Get text sigles
  if ($line =~ m!^([^\/]+\/){2}[^\/]+$!) {
    push @{$data{text}}, $line;
  }

  # Get doc sigles
  elsif ($line =~ m!^[^\/]+\/[^\/]+$!) {
    push @{$data{doc}}, $line;
  }

  # Get corpus sigles
  elsif ($line !~ m!\/!) {
    push @{$data{corpus}}, $line;
  }

  else {
    warn shorten($line) . q! isn't a valid sigle!;
  };
};

# Create collection object
my $json = '{';
$json .= '"@context":"http://korap.ids-mannheim.de/ns/KoralQuery/v0.3/context.jsonld",';
$json .= '"collection":{';

unless (@{$data{corpus}} || @{$data{doc}} || @{$data{text}}) {
  $json .= '}}';
  close($fh);
  print $json;
  exit(0);
};

$json .= '"@type":"koral:docGroup",';
$json .= '"operation":"operation:or",';
$json .= '"operands":[';

foreach my $type (qw/corpus doc text/) {
  unless (@{$data{$type}}) {
    next;
  };
  $json .= '{';
  $json .= '"@type":"koral:doc",';
  $json .= '"key":"' . $type . 'Sigle",';
  $json .= '"match":"match:eq",';
  $json .= '"value":[';
  $json .= join ',', map { '"' . $_ . '"' } @{$data{$type}};
  $json .=  ']';
  $json .= '},';
};

# Remove the last comma
chop $json;

$json .= ']}}';

close($fh);

print $json;

