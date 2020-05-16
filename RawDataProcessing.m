
%{

This file takes in a neural recording from a Neuralynx Cheetah(tm)
converted to a CSV file and processes the input.

The goal of the file is to filter this data to the point for neuron
activity in the range of 70-150 microvolts can be id'ed. 
Then perform clustering of of these voltage spikes across all channels of
the data to find signatures of neuron activity.

First data is loaded from csv, or matlab data file. Then:

Fourier harmonics cleaning

High Pass filtering for low frequencies 600Hz

PCA via SVD

Then K-Means on filtered before and after PCA

%}
function RawDataProcessing()
    main();
end

function main
clear all; close all; clc; dbstop if error;
%data from the Dori Derdikman's Lab, rat neuron recordings, Technion
%Medical Faculty
%{
sampleDir = {
    '2015-01-01_15-45-34_dreadd_rat_ref_animalground_2100depth'; %40 million samples
    '2015-01-29_dreadd_ref17_with_commutator_thresh50';          %15 million samples
    '2015-01-06_130_rat130_arena1' ;                             %50 million samples
    '2015-05-18_messi_before_injection_threshold40_ref4'};%dori  %60 million samples
file = char(sampleDir(4));
%}

outdir =....
        'C:\\Noam\\Output\\doriPlot\\Ohad\\160515\\'; %160519(4) 160515(1)
    
hdir = 'C:\\Noam\\Data\\doriPlot\\Ohad\\160515\\';
sampleDir = {
    '1'; 
    '2';          
    '3';          
    '4'};
file = char(sampleDir(1));

%variables from data recording, channels = # of electrods inserted into rat
Fs = 32000;                   % Sampling frequency
T = 1/Fs;                     % Sample time
startCh = 1;                  % Start channel
numOfCh = 16;                 % End   channel
chunk = 16;                    % Chunk of 1e6 samples 
numChunks = 1;                % Number of chunks %GET AUTO
loadAllChunks = 1;            %load all chunks?
xlabSig='millisecs'; ylabSig='\muV';
xlabFreq='Normalized frequency'; ylabFreq='Magnitude(\muV)';
disp('start')
loadFromFile = 0;


for i=1:length(sampleDir)
    file = char(sampleDir(i));
    tic
    %build spikes segment
    totalSpikes = zeros(1538,1);
    
    if(loadAllChunks == 1)
        chunk = 1;
        numChunks = (length(dir(strcat(hdir,file,'\\chunks'))) - 2)/16; %divides total files by 16 channels, -2 for .. . dirs
    end
    disp(strcat(hdir,file, ' loading ', numChunks, ' chunks!'));
    for chu = chunk:chunk+numChunks-1 %for each chunk
        channel = []; fourier = []; cleanFourierNeg = []; cleanChannelNeg = []; 
        HPfilteredCleanChannel = []; tmp=0; tmp2= 0; tmp3=0; tmp4=0;
        neuralynxTime = [];
        disp(chu);
        %disp('chunk!');
        %disp('ch loaded:');
        neuralynxTime = loadNeuralynxTime(file, i, chu, 1, hdir);
        for i = startCh:(startCh+numOfCh-1) %for each channel in each chunk
            channel(:,i) = loadChunk(file, i, chu, 1, hdir);
            channel(:,i) = channel(:,i)*-1; %invert, makes spikes positive
            channel(:,i) = channel(:,i) -  mean(channel(:,i)); %make 0 mean 
            fprintf('%d|',i);
        end       
        if chu == 1
            save(strcat(outdir,'channels_f',file),'channel');
        end
        time = (1:length(channel))*T*1000; %converts to millisecs
        %figure; plotOffset(time,channel,2000,'input before preprocessing',xlabSig, ylabSig);
        channel = removeNoisyChannels(channel);
        %figure; plotOffset(time,channel,2000,'input noisy channels removed',xlabSig, ylabSig);

        for i = startCh:(startCh+numOfCh-1)
            fourier(:,i) = fft(channel(:,i));
            tmp = cleanHarmonicsNeg(fourier(:,i));%see function descrition
            cleanFourierNeg(:,i) = tmp;
            cleanChannelNeg(:,i) = ifft(tmp);
            HPfilteredCleanChannel(:,i) = highPassFilter(cleanChannelNeg(:,i), Fs);
        end
        % plot fourier before and after after harmonics are cleaned
        ftime = Fs/2*linspace(0,1,(length(fourier(:,1))/2+1));
        tmp = abs(fourier(1:(length(fourier(:,1))/2)+1,1:length(fourier(1,:))));
        %set(gca,'DefaultTextFontSize',32);
        %plot both
        tmp2 = abs(cleanFourierNeg(1:(length(cleanFourierNeg(:,1))/2)+1,1:length(cleanFourierNeg(1,:))));
        %figure; plotOverlayxy(ftime,tmp(:,9),tmp2(:,9), 'Fourier after harmonics cleaned ch 9', xlabFreq, ylabFreq); 
        %legend('original', 'after cleaning harmonics'); ylim([0 11e4]);
        % plot channel after its been cleaned of harmonics
        %original vs cleanes
        %figure; plotOverlayxy(time,channel(:,9), real(cleanChannelNeg(:,9)),'Channel 9: harmonics cleaned',xlabSig, ylabSig);
        %legend('original', 'after cleaning harmonics');
        % plot High Pass
        %figure; plotOffset(time,HPfilteredCleanChannel,500,'Harmonics cleaned and high-pass filtered channels',xlabSig, ylabSig);
        %%%%%%%%%%%%%%%%%   Extracting spikes 
        spikes = extractSpikes(HPfilteredCleanChannel, neuralynxTime, 70, 32, 64); %see function description
        totalSpikes = [totalSpikes, spikes];
        disp('spikes extracted');
        
%         nSpikesPerChannel = zeros(1,16);
        %for i = 1:length(spikes(2,:)) %spikes per channel
           % nSpikesPerChannel(spikes(2,i)) = nSpikesPerChannel(spikes(2,i)) + 1;
        %end
        %plot spikes per channel
        %figure; bar(nSpikesPerChannel./sum(nSpikesPerChannel)*100), title('% spikes per channel'); % which channel had the most spikes
        %cleaning high values
%         vthreshold = 400; %max micro volt threshold
%         signals = spikes(3:end,:);
%         signals(signals>vthreshold)=[vthreshold];
%         signals(signals<-vthreshold)=[-vthreshold];                
    end %chunk loop
    disp('done!');
    toc
    save(strcat(outdir,'totalSpikes_',file),'totalSpikes');
end %file loop


STOP


%loading from file (processed from raw data using NeuralDataBinaryToInteger.java script) 
if loadFromFile == 1
    disp('ch loaded:');
    for i = startCh:(startCh+numOfCh-1)
        channel(:,i) = loadChunk(file, i, chunk, numChunks, hdir);
        channel(:,i) = channel(:,i)*-1; %invert, makes spikes positive
        channel(:,i) = channel(:,i) -  mean(channel(:,i));
        figure; plotOffset(time,channel,500,'Input Signal: Raw Neural Recording of Rat',xlabSig, ylabSig);
        %1 channel
        figure; plotOffset(time,channel(:,9),500,'Channel 9: input Signal',xlabSig, ylabSig);
        fprintf('%d|',i);
    end
end
%}
%{ for Demo these values are preprocessed
if loadFromFile == 0
    load('demo_channels');
    load('demo_fourier');
    load('demo_fourierCleanlNeg');
    load('demo_cleanChannelNeg');
    load('demo_HPfilteredCleanChannel');
    load('demo_spikes');
end
%}

% Time
time = (1:length(channel(:,1)))*T*1000; %converts to millisecs

%%%%%%%%%%%%%%%%%%%plot input
%all
figure; plotOffset(time,channel,500,'Input Signal: Raw Neural Recording of Rat',xlabSig, ylabSig);
%1 channel
figure; plotOffset(time,channel(:,9),500,'Channel 9: input Signal',xlabSig, ylabSig);


%%%%%%%%%%%%%%%%%%%% Fourier Cleaning
%used the built in fn, we have a DFT implementation included (DFT_Fun.m) 
%but we did not use it as specified by Freddy.
if loadFromFile == 1
    for i = startCh:(startCh+numOfCh-1)
        fourier(:,i) = fft(channel(:,i));
    end
end
%fourier cleaning (negatives)
for i = startCh:(startCh+numOfCh-1)
    tmp = cleanHarmonicsNeg(fourier(:,i));%see function descrition
    cleanFourierNeg(:,i) = tmp;
    cleanChannelNeg(:,i) = ifft(tmp);
end
tmp = 0;
disp('cleaned');

% plot fourier before and after after harmonics are cleaned
ftime = Fs/2*linspace(0,1,(length(fourier(:,1))/2+1));
tmp = abs(fourier(1:(length(fourier(:,1))/2)+1,1:length(fourier(1,:))));
set(gca,'DefaultTextFontSize',32);
figure; plot1xy(ftime, tmp(:,9), 'Fourier of input signal', xlabFreq, ylabFreq);
%plot both
tmp2 = abs(cleanFourierNeg(1:(length(cleanFourierNeg(:,1))/2)+1,1:length(cleanFourierNeg(1,:))));
figure; plotOverlayxy(ftime,tmp(:,9),tmp2(:,9), 'Fourier after harmonics cleaned', xlabFreq, ylabFreq);
legend('original', 'after cleaning harmonics');

% plot channel after its been cleaned of harmonics
figure; plotOffset(time,cleanChannelNeg,500,'Harmonics cleaned channels',xlabSig, ylabSig);
%just channel 9
figure; plotOffset(time,cleanChannelNeg(:,9),500,'Channel 9: harmonics cleaned',xlabSig, ylabSig);
%original vs cleanes
figure; plotOverlayxy(time,channel(:,9), real(cleanChannelNeg(:,9)),'Channel 9: harmonics cleaned',xlabSig, ylabSig);
legend('original', 'after cleaning harmonics');


%%%%%%%%%%%%%% High Pass filtering
%hp only, for debug
if loadFromFile == 1
    for i = startCh:(startCh+numOfCh-1)
        %HPfilteredChannel(:,i) = highPassFilter(channel(:,i), Fs);
    end
end

% hp filter plus cleaned
if loadFromFile == 1
    for i = startCh:(startCh+numOfCh-1)
        HPfilteredCleanChannel(:,i) = highPassFilter(cleanChannelNeg(:,i), Fs);
    end
end
disp('filtered');
%plotting fourier original, cleaning harmonics, and highpass filtering.
tmp3=fft(HPfilteredCleanChannel(:,9)); tmp4 = abs(tmp3(1:(length(tmp3)/2)+1));
figure;
hold on
plot1xy(ftime,tmp(:,9), '', xlabFreq, ylabFreq);
plot1xy(ftime,tmp2(:,9), '', xlabFreq, ylabFreq);
plot1xy(ftime,tmp4, 'Fourier after Highpass Filtering', xlabFreq, ylabFreq);
hold off
legend('original', 'after cleaning harmonics', 'after highpass filter of 600Hz');
tmp = 0; tmp2 = 0, tmp3 = 0, tmp4 = 0;

% plot High Pass
figure; plotOffset(time,HPfilteredCleanChannel,500,'Harmonics cleaned and high-pass filtered channels',xlabSig, ylabSig);
% Channel 9
figure; plot1xy(time,HPfilteredCleanChannel(:,9),'Channel 9: harmonics cleaned and filtered', xlabSig, ylabSig);
% overlay
figure; plotOverlayxy(time,real(channel(:,9)), real(HPfilteredCleanChannel(:,9)),'Channel 9: after harmonics cleaned and filtered',xlabSig, ylabSig);
legend('original', 'after cleaning and highpass filter');

%%%%%%%%%%%%%%%%%   Extracting spikes 
if loadFromFile == 1 
    spikes = extractSpikes(HPfilteredCleanChannel, 70, 32, 64); %see function description
end
disp('spiked extracted');
nSpikesPerChannel = zeros(1,16);
for i = 1:length(spikes(2,:)) %spikes per channel
    nSpikesPerChannel(spikes(2,i)) = nSpikesPerChannel(spikes(2,i)) + 1;
end
%plot spikes per channel
figure; bar(nSpikesPerChannel./sum(nSpikesPerChannel)*100), title('% spikes per channel'); % which channel had the most spikes
%cleaning high values
vthreshold = 400; %max micro volt threshold
signals = spikes(3:end,:);
signals(signals>vthreshold)=[vthreshold];
signals(signals<-vthreshold)=[-vthreshold];


STOP


%%%%%%%%%%%%%%          kmeans input
nKlusters = 6; %initialize with 6 clusters (depends on data, this is from Dori's lab)
tic
sampleByKlusterID = kmeans(signals',nKlusters);
disp('elapsed time K-means on input');
tIn = toc
[n , p] = hist(sampleByKlusterID,nKlusters); bar(1:nKlusters,n./sum(n)*100),...
    title(sprintf('kmeans on events k=%d non-svd', nKlusters));
ylabel('% of total events'); xlabel('cluster id');
sampleLength = 96;%3ms: 1ms before spike, 2ms after
%plot clusters
figureOffset = 100;
buildClusters(signals, numOfCh, sampleLength, nKlusters, sampleByKlusterID,'non-svd',figureOffset);

%%%%%%%%%%%%%PCA - Single Value Decomposition %SVD%
%make sure data matrix is set up so samples are COL
%u is basis vector
%then multiply first n COL of u transpose times datam atrix to change base 
%do svd on that.
[u , s , v] = svd(signals,'econ');
figure; plot(s), title('Singular values of data SVD');
nUvectors = 40;
pcaSpikes = u(:,1:nUvectors)'*signals;%converts spikes to new base using only u eigen vectors
%vectors back in original base
originalBasisSpikes = u(:,1:nUvectors)* pcaSpikes;
mse = mean(mean((signals(:,:)- originalBasisSpikes(:,:)).^2));
%memory usage
figure; bar([100,numel(pcaSpikes)/numel(signals)*100]), 
title(sprintf('Storage of original signal vs. SVD using %d u vectors',nUvectors));
set(gca,'XTick',1:2,'XTickLabel',{'original';'pca'}), ylabel('% of original');
%figure original signal vs pca after converting back to original base
figure;
hold on;
plot(signals(:,700));
plot(originalBasisSpikes(:,700));
hold off;
title(sprintf('Sample of original vs SVD Vector using %d u vectors\n(average MSE for all samples %.2f)',nUvectors,mse));
legend('original', 'svd');
%sanity check MSE per # of u's, plot MSE's
for ui=1:10:length(u)
    pcaSpikes2 = u(:,1:ui)'*signals;
    originalBasisSpikes2 = u(:,1:ui)* pcaSpikes2;
    uii = ui;
    if(ui > 1)
        uii = (ui-1)/10 +1;
    end
    mseplot(1,uii) = mean(mean((signals(:,:)- originalBasisSpikes2(:,:)).^2));
    mseplot(2,uii) = ui;
end
figure;plot(mseplot(2,:),mseplot(1,:)); title('MSE per number of SVD u vectors');
ylabel('mse'), xlabel('number of u vectors');

%%%%%%%%%%%%%%%%%%% K-Means after PCA
nKlustersPCA = 6;
tic
sampleByKlusterID = kmeans(pcaSpikes',nKlustersPCA);
disp('elapsed time K-means on pca');
tPca = toc;
figure; bar([tIn,tPca]), 
title(sprintf('Time of K-Means on original vs SVD using %d u vectors',nUvectors));
set(gca,'XTick',1:2,'XTickLabel',{'original';'pca'}), ylabel('sec');
%spikes per cluster pca
figure; hist(sampleByKlusterID), title('number of kmeans clusters pca signals');
figure; [n , p] = hist(sampleByKlusterID,nKlustersPCA); bar((1:nKlustersPCA),n./sum(n)*100), ...
    title(sprintf('kmeans on events k=%d SVD', nKlustersPCA));
ylabel('% of total events'); xlabel('cluster id');

%%% sort PCA signals by cluster and plot cluster
buildClusters(signals, numOfCh, sampleLength, nKlusters, sampleByKlusterID,'svd',200);

stop %meant to crash here, dbstop command above stops debugger with vars in memory 
end


%%%%%%%%%%%SUBROUTINES%%%%%%%%%%

function channels = removeNoisyChannels(channels)
    a = std(channels); 
    thresh = 5; % thresh * std's of median removed
    %figure;bar(a); title('STDs');
    for i = 1:length(channels(1,:))
        if a(i) > thresh * median(a);
        channels(:,i) = ones(length(channels(:,1)),1);
        end
    end
end

%creates data structure of samples per cluster and plots them based on
%output after running kmeans
%klusterIds is matrix of each sample and which cluster it belongs to
function buildClusters(signals,nChannels, sampleLength, nKlusters,...
    klusterIds,title,figureOffset)
    %%% sort signals by cluster
    clusters = cell(nKlusters,nChannels);
    numSpikesPerCluster=zeros(nKlusters,1);
    for k = 1:length(klusterIds)
      numSpikesPerCluster(klusterIds(k)) = numSpikesPerCluster(klusterIds(k))+1;
      for ch = 1:nChannels %16 channels
        clusters{klusterIds(k),ch}(end+1,:) = signals(1+(ch-1)*sampleLength:ch*sampleLength,k);
      end
    end

    %%%plots mean and std of alls signals per cluster
    %close all;
    for k = 1:size(clusters,1)
        figure(k+figureOffset);%does not overwrite other clusters
        for ch = 1:size(clusters,2)
            if length(clusters{k,1}) > 100%ignore small clusters
                subplot(4,4,ch)%plots all channels
                for sam = 1:sampleLength
                    cmean(k,ch,1,sam) = mean(clusters{k,ch}(:,sam));
                    cmean(k,ch,2,sam) =  std(clusters{k,ch}(:,sam));
                end
                errorbar(cmean(k,ch,1,:), cmean(k,ch,2,:)),
                axis([1,98,-150,150]);%limits of voltage interested in
            end
        end
        suptitle(sprintf(' %s: kluster:%d channel:%d n-spikes:%d\n y=microvolts x=time',...
            title,k, ch,numSpikesPerCluster(k)));
        %saveas(gcf,sprintf('demo_%s_mean_k_%d.png',title,k)); %saves to file
    end

end

%finds voltage spikes above threshold in signal
%stores window of time before/after spike
%
%data: each channel is column vectors
%start at first ms so window doesn't crash
%data matrix, rows = sample structure, cols = samples
function spikes = extractSpikes(data, neuralynxTime, thresholdMiV, windowBeforeMS, windowAfterMS)
    eventIntervalThresh = 32;%1ms between events
    spikes = zeros(2+(windowBeforeMS+windowAfterMS)*16,10e3); %max number make larger
    numSpikes = 2;%offset first value so we can do ,numSpikes-1
    prevSpikeTime = -eventIntervalThresh;
    for timei = windowBeforeMS+1 : length(data(:,1))-windowAfterMS %start at edge of back window, end edge forward window 
       saved = 0;
       %channel loop
       for channeli = 1: length(data(1,:)) %checks each channel for a spike
           if (data(timei,channeli) > thresholdMiV) && ... 
           (timei > prevSpikeTime + eventIntervalThresh) %(spikes(1,numSpikes-1)+ eventIntervalThresh)) %min space between event intervals
              saved = 1; %don't save spikes on multiple channels, unused
              prevSpikeTime = timei;
              spikes(1:2,numSpikes) = [neuralynxTime(timei);channeli]; %store time and channel of spike recorded //timei
              for chi2 = 1: length(data(1,:))  %stores information on all channels when spike is found
                  %storing time and channel is first two values so 2 offset
                  %extracts window per channel 
                  %+2 offset time/channel in beginning
                  % (chi2-1)* and chi2* before/after window at length of channel. 
                  spikes((1+2+(chi2-1)*(windowBeforeMS+windowAfterMS)):2+chi2*(windowBeforeMS+windowAfterMS),numSpikes) = ...
                      real(data((timei-windowBeforeMS):(timei+windowAfterMS-1),chi2)); %32 samples per ms
              end
              numSpikes = numSpikes + 1;
           end
       end
    end
    spikes = spikes(:,2:numSpikes-1); %trims off all 0's at end and one at beginning
end

% Clean Harmonics
%this function cleans harmonics by picking a starting point(offset) after
%which any value above a threashold (mean*factor) is reset to mean.
function out=cleanHarmonicsNeg(input)
offset = 2*1e4; %when to start cleaning, this is determined emperically for now 
% looking at the original fourier and seeing that harmonics do not start until these frequencies
tmp=abs(input);
out=input;
factor = 3; %factor times mean to set threshold for cutting (determined experimentally)
period = 100;%the period over which to determine the average 
sumPer = sum(tmp(offset+1:offset+period)); 
for i = offset+period+1:length(tmp) 
    if tmp(i)>factor*sumPer/period %mean over period
        tmp(i)=sumPer/period;  %cuts out and changes value to mean
        out(i)= sumPer/period;
        if out(i)<0
            out(i)= -1*sumPer/period;
        end
    end
    sumPer=sumPer-tmp(i-period)+tmp(i);
end
end

function data = loadChannel(file, channel)
hdir = 'C:\\Users\\alm\\Desktop\\dori\\raw_data';
ADBitVolts = 0.000000015624999960550667;%conversion of raw data to volts
data = load(sprintf('%s\\%s\\ch%d.csv',hdir,file,channel));
data = data*ADBitVolts; % to volts
data = data*1e6; % to micro volts
end


function data = loadNeuralynxTime(file, channel, chunk, numChunks, hdir)
    data = [];
    for i = 1:numChunks
        %fprintf('%s\\%s\\chunks\\ch%d_%d.csv',hdir,file,channel,chunk+i-1);
        tmp = load(sprintf('%s\\%s\\chunks\\ch%d_%d.csv',hdir,file,channel,chunk+i-1));
        %data((i-1)*1e6+1:i*1e6) = tmp(:,2);
        data = [data(:) ; tmp(:,1)];
    end
end

%chunks are 1e6 long consequetive parts of the channel signal, can load
%multiple chunks
function data = loadChunk(file, channel, chunk, numChunks, hdir)
    ADBitVolts = 0.000000015624999960550667;%conversion of raw data to volts
    %data = zeros(1e6*numChunks,1); %chunks are 1e6 long, preallocates
    data = [];
    prev = 2;
    for i = 1:numChunks
        %fprintf('%s\\%s\\chunks\\ch%d_%d.csv',hdir,file,channel,chunk+i-1);
        tmp = load(sprintf('%s\\%s\\chunks\\ch%d_%d.csv',hdir,file,channel,chunk+i-1));
        %data((i-1)*1e6+1:i*1e6) = tmp(:,2);
        data = [data(:) ; tmp(:,2)];
    end
    data = data*ADBitVolts; % to volts % to volts 
    data = data*1e6; % to micro volts
    %data = data';
end

function data = highPassFilter(input, samplingFrequency)
%HIGH PASS FILTERING
% http://www.mathworks.com/help/dsp/ref/fdesign.bandpass.html
% All frequency values are in Hz.
% Construct an FDESIGN object and call its BUTTER method.
Fstop =590;           % First Stopband Frequency
Fpass =600;           % First Passband Frequency This value is used because it interferes with neuron activity.
Astop = 10;          % First Stopband Attenuation (dB)
Apass  = 5;           % Passband Ripple (dB)
fs = samplingFrequency;
%toDecibal = 20*log(10);         
Hd = design(fdesign.highpass(Fstop, Fpass, Astop, Apass, fs),'butter');
data = filtfilt(Hd.sosMatrix,Hd.ScaleValues,input);
end


%%%%%%%%%PLOTTING FUNCTIONS%%%%%%%%%%

function plot1xy(x,y,tit, xlab, ylab)
    plot(x,y)
    title(tit)
    xlabel(xlab)
    ylabel(ylab)
end

function plot1(y, tit, xlab, ylab)
    plot(y)
    title(tit)
    xlabel(xlab)
    ylabel(ylab)
end

%for plotting frequency
function plotOverlay(data, data2, tit, xlab, ylab, cols)

nplots = size(data,2);
if nplots > 1
    for i = 1:nplots %assumes 4 plots/adata
        subplot(nplots/cols,cols,i);
        hold on
        plot(data(:,i))
        plot(data2(:,i))
        hold off
        title(tit)
        xlabel(xlab)
        ylabel(ylab)
    end
else
     hold on
     plot(data)
     plot(data2)
     hold off
end
title(tit)
xlabel(xlab)
ylabel(ylab)
end

%for plotting frequency
function plotOverlayxy(x,data, data2, tit, xlab, ylab, cols)

nplots = size(data,2);
if nplots > 1
    for i = 1:nplots 
        subplot(nplots/cols,cols,i);
        hold on
        plot(x,data(:,i))
        plot(x,data2(:,i))
        hold off
        title(tit)
        xlabel(xlab)
        ylabel(ylab)
    end
else
     hold on
     plot(x,data);
     plot(x,data2);
     hold off
end
title(tit)
xlabel(xlab)
ylabel(ylab)
end



%for plotting frequency
function plotSubplots(data, tit, xlab, ylab, cols)
nplots = size(data,2);
    for i = 1:nplots %assumes 4 plots/adata
        subplot(nplots/cols,cols,i);
        plot(data(:,i))
        title(tit)
        xlabel(xlab)
        ylabel(ylab)
    end
end

%for plotting signal, plots all sets vertically offset
function plotOffset(time, data, offset, tit, xlab, ylab)
    hold on;
    for i = 1:length(data(1,:))%length(data(1,:)):-1:1
         plot(time,data(:,i)+(i-1)*offset)
         %plot(time,ones(1,length(data(:,i)))*(i-1)*offset)
    end
    hold off
    title(tit);
    xlabel(xlab);
    ylabel(ylab);
    legend('show');
    %n=get(gca,'Ytick');
    %set(gca,'yticklabel',sprintf('%.0f',n'));
end


%for plotting frequency
function plot4subplotsOverlay(data, data2, tit, xlab, ylab)
    for i = 1:4 %assumes 4 plots/adata
        subplot(2,2,i);
        hold on
        plot(data(:,i))
        plot(data2(:,i))
        hold off
        title(tit)
        xlabel(xlab)
        ylabel(ylab)
    end
end

%for plotting frequency
function plot4subplots(data, tit, xlab, ylab)
    for i = 1:4 %assumes 4 plots/adata
        subplot(2,2,i);
        plot(data(:,i))
        title(tit)
        xlabel(xlab)
        ylabel(ylab)
    end
end
